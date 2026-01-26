import discord
import json
import asyncio
import aiohttp
import io
import opuslib
import webrtcvad
import audioop
from discord.ext import commands
from discord import ui
import dotenv
import os

dotenv.load_dotenv()

TOKEN = os.getenv("DISCORD_BOT_TOKEN")
BACKEND_URL = os.getenv("BACKEND_URL")
GUILDS = json.loads(os.getenv("GUILDS", "[]"))
CONSENT_FILE = os.getenv("CONSENT_FILE")
IGNORED_CHANNELS = json.loads(os.getenv("IGNORED_CHANNELS", "[]"))

# Load consented users
try:
    with open(CONSENT_FILE) as f:
        consented_users = set(json.load(f))
except FileNotFoundError:
    consented_users = set()

def save_consented():
    with open(CONSENT_FILE, 'w') as f:
        json.dump(list(consented_users), f)

intents = discord.Intents.default()
intents.voice_states = True
intents.members = True

bot = commands.Bot(command_prefix='!', intents=intents)

class AcceptButton(ui.View):
    def __init__(self, user_id):
        super().__init__()
        self.user_id = user_id

    @ui.button(label="Accept TOS and Privacy Policy", style=discord.ButtonStyle.primary)
    async def accept(self, interaction: discord.Interaction, button: ui.Button):
        if interaction.user.id == self.user_id:
            consented_users.add(str(self.user_id))
            save_consented()
            await interaction.response.send_message("You have accepted the TOS and Privacy Policy. You can now speak in voice channels.", ephemeral=True)
            # Unmute the user
            for guild in bot.guilds:
                if str(guild.id) in GUILDS:
                    member = guild.get_member(self.user_id)
                    if member and member.voice and member.voice.channel:
                        await member.edit(mute=False)
        else:
            await interaction.response.send_message("This is not for you.", ephemeral=True)

@bot.event
async def on_voice_state_update(member, before, after):
    if member == bot.user:
        return  # Ignore bot's own voice state changes
    if before.channel != after.channel:
        if after.channel:  # Someone joined a channel
            # Check if bot should join
            if str(after.channel.id) in IGNORED_CHANNELS:
                return  # Don't join ignored channels
            # Find the channel with the most people (excluding ignored)
            guild = after.channel.guild
            voice_channels = [ch for ch in guild.voice_channels if str(ch.id) not in IGNORED_CHANNELS and ch != guild.afk_channel]
            if not voice_channels:
                return
            target_channel = max(voice_channels, key=lambda ch: len([m for m in ch.members if not m.bot]))
            # If bot is not in any VC, join the target channel
            if not bot.voice_clients:
                vc = await target_channel.connect()
            else:
                # If already in a VC, check if it's the target
                current_vc = bot.voice_clients[0]
                if current_vc.channel != target_channel:
                    await current_vc.disconnect()
                    vc = await target_channel.connect()
                else:
                    vc = current_vc
            # Now handle the member
            if str(member.id) not in consented_users:
                # Mute the user
                await member.edit(mute=True)
                # Send DM
                try:
                    embed = discord.Embed(title="Voice Guard", description="To speak in voice channels, you must accept our TOS and Privacy Policy.\n\n[TOS and Privacy Policy](https://tos.outcraft.net)", color=0x00ff00)
                    view = AcceptButton(member.id)
                    await member.send(embed=embed, view=view)
                except discord.Forbidden:
                    pass  # Can't DM
            else:
                # Start streaming audio
                await start_audio_stream(vc, member)

async def start_audio_stream(vc, member):
    # Use Pycord's voice receive with sink
    from discord.sinks import WaveSink
    
    class CustomSink(WaveSink):
        def __init__(self, member_to_track):
            super().__init__()
            self.member_to_track = member_to_track
            self.vad = webrtcvad.Vad(3)
            self.encoder = opuslib.Encoder(48000, 2, 'voip')
            self.audio_buffer = {}
            
        @WaveSink.listener()
        def on_data(self, user, data):
            if user.id != self.member_to_track.id:
                return
                
            # Initialize user buffer if needed
            if user.id not in self.audio_buffer:
                self.audio_buffer[user.id] = {'buffer': b'', 'speaking': False}
            
            user_buf = self.audio_buffer[user.id]
            
            # Data is already PCM
            pcm_data = data
            
            # Convert to mono for VAD check
            try:
                mono_data = audioop.tomono(pcm_data, 2, 0.5, 0.5)
                is_speech = self.vad.is_speech(mono_data[:960], 48000)
            except Exception as e:
                return
            
            if is_speech and not user_buf['speaking']:
                user_buf['speaking'] = True
                user_buf['buffer'] = pcm_data
            elif is_speech:
                user_buf['buffer'] += pcm_data
            elif user_buf['speaking']:
                user_buf['speaking'] = False
                # Send to backend
                asyncio.create_task(self.send_audio(user_buf['buffer'], self.member_to_track))
                user_buf['buffer'] = b''
        
        async def send_audio(self, pcm_data, member):
            # Encode to opus
            frame_size = 960
            opus_packets = []
            for i in range(0, len(pcm_data), frame_size * 4):
                chunk = pcm_data[i:i + frame_size * 4]
                if len(chunk) < frame_size * 4:
                    chunk += b'\x00' * (frame_size * 4 - len(chunk))
                try:
                    opus_packet = self.encoder.encode(chunk, frame_size)
                    opus_packets.append(opus_packet)
                except:
                    continue
            
            if not opus_packets:
                return
            
            opus_data = b''.join(opus_packets)
            
            async with aiohttp.ClientSession() as session:
                form_data = aiohttp.FormData()
                form_data.add_field('audio', io.BytesIO(opus_data), filename='audio.opus')
                form_data.add_field('player_uuid', str(member.id))
                form_data.add_field('player_name', member.display_name)
                form_data.add_field('server_uuid', str(member.guild.id))
                try:
                    async with session.post(f"{BACKEND_URL}/analyze", data=form_data) as resp:
                        pass
                except Exception as e:
                    print(f"Error sending to backend: {e}")
    
    sink = CustomSink(member)
    try:
        vc.start_recording(sink, lambda: None, lambda: None)
    except Exception as e:
        print(f"Error starting voice receive: {e}")

@bot.event
async def on_ready():
    print(f'Bot is ready as {bot.user}')
    bot.loop.create_task(check_disconnect())

async def check_disconnect():
    while True:
        await asyncio.sleep(300)  # Check every 5 minutes
        for vc in bot.voice_clients[:]:  # Copy list to avoid modification issues
            guild = vc.guild
            # Check if any voice channel in the guild has non-bot members (excluding ignored and AFK)
            has_members = any(
                len([m for m in ch.members if not m.bot]) > 0
                for ch in guild.voice_channels
                if str(ch.id) not in IGNORED_CHANNELS and ch != guild.afk_channel
            )
            if not has_members:
                await vc.disconnect()

bot.run(TOKEN)