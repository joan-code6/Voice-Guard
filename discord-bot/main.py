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
        if after.channel:  # Joined a channel
            # Check if bot is already in the channel
            vc = None
            for v in bot.voice_clients:
                if v.channel == after.channel:
                    vc = v
                    break
            if not vc:
                vc = await after.channel.connect()
            # Check if consented
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
    # Use a custom sink for audio
    class AudioSink(discord.sinks.Sink):
        def __init__(self, member):
            super().__init__()
            self.member = member
            self.vad = webrtcvad.Vad(3)  # Aggressiveness 0-3
            self.encoder = opuslib.Encoder(48000, 2, 'voip')  # Stereo opus encoder
            self.buffer = b''
            self.speaking = False

        def write(self, user, data):
            if user != self.member:
                return
            # data is PCM 48kHz 16-bit stereo
            # Convert to mono for VAD
            mono_data = audioop.tomono(data, 2, 0.5, 0.5)
            # Check VAD
            is_speech = self.vad.is_speech(mono_data, 48000)
            if is_speech and not self.speaking:
                self.speaking = True
                self.buffer = data
            elif is_speech:
                self.buffer += data
            elif self.speaking:
                self.speaking = False
                # Send buffer to backend
                asyncio.create_task(self.send_to_backend(self.buffer))
                self.buffer = b''

        async def send_to_backend(self, pcm_data):
            # Encode to opus
            frame_size = 960  # 20ms at 48kHz
            opus_packets = []
            for i in range(0, len(pcm_data), frame_size * 4):  # 4 bytes per sample stereo
                chunk = pcm_data[i:i + frame_size * 4]
                if len(chunk) < frame_size * 4:
                    chunk += b'\x00' * (frame_size * 4 - len(chunk))
                opus_packet = self.encoder.encode(chunk, frame_size)
                opus_packets.append(opus_packet)
            opus_data = b''.join(opus_packets)
            # Send to backend
            async with aiohttp.ClientSession() as session:
                data = aiohttp.FormData()
                data.add_field('audio', io.BytesIO(opus_data), filename='audio.opus')
                data.add_field('player_uuid', str(self.member.id))
                data.add_field('player_name', self.member.display_name)
                data.add_field('server_uuid', str(self.member.guild.id))
                try:
                    async with session.post(f"{BACKEND_URL}/analyze", data=data) as resp:
                        pass  # Maybe log response
                except Exception as e:
                    print(f"Error sending to backend: {e}")

    sink = AudioSink(member)
    vc.listen(sink)

@bot.event
async def on_ready():
    print(f'Bot is ready as {bot.user}')

bot.run(TOKEN)