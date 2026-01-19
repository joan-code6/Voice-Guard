The Idea of this Project is to Create a plugin for Minecraft Java 1.20.6 which will take all voice input from players throught simple voice chat and run it through an speach to text model to check for certain bad words which get configured in a config. If a player says one of the bad words the plugin should only store the audio (30 sek before the word was said and 15 sek after) and log it in the console.

Infra:

the plugin should collect all inputs from players and then send them to a python backend which will then pass it to whisper and check for bad words. On the python server things should be logged in a database and the audio files should be stored in a folder.