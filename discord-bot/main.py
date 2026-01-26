import discord
import json
import asyncio
import aiohttp
import io
import opuslib
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
    async def accept(self, button: ui.Button, interaction: discord.Interaction):
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
    # Create a custom voice receiver
    class VoiceReceiver:
        def __init__(self, vc, member):
            self.vc = vc
            self.member = member
            self.encoder = opuslib.Encoder(48000, 2, 'voip')
            self.running = True
            
        async def listen(self):
            while self.running and self.vc.is_connected():
                try:
                    # Access raw voice packets
                    if hasattr(self.vc, 'recv_audio_packet'):
                        packet = await self.vc.recv_audio_packet()
                        if packet and packet.ssrc == self.member.id:
                            await self.process_audio(packet.decrypted_data)
                    else:
                        # discord.py doesn't support voice receive, log once
                        print("Voice receiving not supported with current discord.py version. Please install py-cord.")
                        self.running = False
                        break
                    await asyncio.sleep(0.02)  # 20ms
                except Exception as e:
                    print(f"Error in voice receiver: {e}")
                    await asyncio.sleep(1)
                    
        async def process_audio(self, pcm_data):
            try:
                await self.send_to_backend(pcm_data)
            except:
                pass
                
        async def send_to_backend(self, pcm_data):
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
                form_data.add_field('player_uuid', str(self.member.id))
                form_data.add_field('player_name', self.member.display_name)
                form_data.add_field('server_uuid', str(self.member.guild.id))
                try:
                    async with session.post(f"{BACKEND_URL}/analyze", data=form_data) as resp:
                        print(f"Sent audio to backend: {resp.status}")
                except Exception as e:
                    print(f"Error sending to backend: {e}")
    
    receiver = VoiceReceiver(vc, member)
    asyncio.create_task(receiver.listen())

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