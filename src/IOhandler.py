# This example requires the 'message_content' intent.
import subprocess
import discord
from discord.ext import commands
import logging
from dotenv import load_dotenv
import os

load_dotenv()
token = os.getenv('DISCORD_TOKEN')

handler = logging.FileHandler(filename='discord.log', encoding='utf-8', mode='w')
intents = discord.Intents.default()
intents.message_content = True
intents.members = True

bot = commands.Bot(command_prefix='!', intents=intents)


@bot.event
async def on_ready():
    print(f"We are ready to go in, {bot.user.name}")


@bot.event
async def on_message(message):
    if message.author == bot.user:
        if message.content[0:17] == "New game created!":
            global rootMsg
            rootMsg = message
        else:
            return

    await bot.process_commands(message)


@bot.command()  # !hello command for basic functionality test
async def hello(ctx):
    # print("hello received")
    await ctx.send(f"Hello {ctx.author.mention}!")


@bot.command()
async def about(ctx):
    try:
        with open('about.md', 'r', encoding='utf-8') as f:
            # About message is sent in multiple chunks to skirt Discord's character limit
            msgs = f.read().split("%")
            for msg in msgs:
                await ctx.send(msg)
    except Exception as e:
        print(f"Error: {e}")

rootMsg: discord.message


@bot.command()
async def new_game(ctx):
    await ctx.send("New game created!\n"
                   "Player assignments are as follows:\n"
                   "AUSTRIA: UNCLAIMED\n"
                   "ENGLAND: UNCLAIMED\n"
                   "FRANCE: UNCLAIMED\n"
                   "GERMANY: UNCLAIMED\n"
                   "ITALY: UNCLAIMED\n"
                   "RUSSIA: UNCLAIMED\n"
                   "TURKEY: UNCLAIMED\n"
                   "\n"
                   "Use **!claim country_name** to claim a country. For instance:\n"
                   "> !claim FRANCE\n"
                   "to claim France.\n")
    try:
        for filename in os.listdir("../current/orders"):
            with open(f"../current/orders/{filename}", 'w', encoding='utf-8') as f:
                f.write("UNCLAIMED")
    except Exception as e:
        print(f"Exception: {e}")


@bot.command()
async def claim(ctx, arg):
    print(f"Player attempting to claim a country: {arg}")
    filepath = os.path.join(f"../current/orders/{arg}.txt")
    try:
        with open(filepath, 'r+', encoding='utf-8') as f:
            if f.read() == "UNCLAIMED":
                # Store this User's id in their country's orders file
                f.seek(0)
                f.write(str(ctx.author.id))
                f.truncate()
                # Edit the game's root message to reflect this user claiming this country
                new_msg = ""
                for line in rootMsg.content.split("\n"):
                    if line.startswith(arg):
                        new_msg += arg+": "+ctx.author.mention+"\n"
                    else:
                        new_msg += line+"\n"
                await rootMsg.edit(content=new_msg)
                # Open a DM with the claiming player
                await ctx.author.send(f"You've just claimed {arg}! Send your orders for this game in this DM. Good luck!")
            else:
                await ctx.send(f"{arg} has already been claimed")
    except Exception as e:
        print(f"Exception: {e}")


@bot.command()
async def orders(ctx, *, message: str):
    if not isinstance(ctx.channel, discord.DMChannel): # Prevent players from leaking their orders publicly
        await ctx.message.delete()
        await ctx.send("For your own privacy, please send orders in DMs")
        return
    try:
        file_names = os.listdir("../current/orders")
        for file_name in file_names: # Find which file to write the player's orders into
            with open(f"../current/orders/{file_name}", 'r+', encoding='utf-8') as f:
                try:
                    country_id = int(f.read().split("\n")[0])
                except ValueError:
                    country_id = -1
                if country_id == ctx.author.id: # Check that this orders file has been claimed by this player
                    # Write the player's orders to the file:
                    f.seek(0)
                    f.write(str(country_id)+"\n")
                    f.write(message)
                    f.truncate()
                    await ctx.send("Orders received!")
                    return
    except Exception as e:
        print(f"Exception: {e}")

java_process: subprocess.Popen


@bot.command()
async def reveal(ctx):
    result = ""
    global java_process
    try:
        java_process = subprocess.Popen(
            ['java','-cp', 'out/production/Diplomat','Main'],
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            bufsize=1,
            cwd=os.getcwd()+"/.."
        )
        while True:
            line = java_process.stdout.readline()
            if not line:
                break
            clean_line = line.strip()
            if clean_line == "XXX":
                break
            # collect the output from the subprocess in result
            result += clean_line+"\n"
            print(clean_line, flush=True)
    except Exception as e:
        print(f"Exception: {e}")
    # send the collected output of the process
    await ctx.send(result)


@bot.command()
async def reveal_retreats(ctx):
    global java_process
    if java_process.poll() is not None:
        await ctx.send("No retreats to report now")
        return
    java_process.stdin.write("GO\n")
    result = ""
    while True:
        line = java_process.stdout.readline()
        clean_line = line.strip()
        if clean_line == "XXX":
            break
        result += clean_line+"\n"
        print(clean_line, flush=True)
    await ctx.send(result)


@bot.command()
async def reveal_builds(ctx):
    global java_process
    if java_process.poll() is not None:
        await ctx.send("No builds to report now")
        return
    java_process.stdin.write("GO\n")
    result = ""
    while True:
        line = java_process.stdout.readline()
        if not line:
            break
        clean_line = line.strip()
        if clean_line == "XXX":
            break
        result += clean_line+"\n"
        print(clean_line, flush=True)
    await ctx.send(result)

bot.run(token, log_handler=handler, log_level=logging.DEBUG)
