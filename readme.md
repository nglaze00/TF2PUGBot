# TF2 Pick-up Game Bot
Discord bot that automates queueing, team balancing, class selection, and game server configuration for Team Fortress 2 pickup games (PUGs). Written in **Java** using  [**JDA**](https://github.com/DV8FromTheWorld/JDA) (Java Discord API) and [**AGQL**](https://github.com/ribasco/async-gamequery-lib) (API for TF2 server configuration); stores player data in **MySQL** database.

## Overview
Pick-up games, or competitive matches between teams of individual, unaffiliated players (as opposed to organized teams) are a staple of any video game with a competitive scene. TF2 is no different, but, since the game has a relatively smaller competitive player base, the current most popular PUG site runs their games manually, with a server moderator performing the entire process on their own.

This bot automates that entire process; instead of having to wait to play until a moderator is present, the bot allows games to be played at any time, and even simultaneously, which is impossible with manually-run games.

The bot uses the **Elo** rating formula to rank players. It also allows players to input which classes they would prefer to play, and the team-balancing algorithm takes each player's rating and class preferences into account when sorting teams for each match.
## Usage
### Playing a PUG
When the bot is added to a server, it will automatically configure it with a queue channel for each of TF2's competitive game formats: Ultiduo (2v2), 4v4, and 6v6.

![](https://github.com/nglaze00/TF2PUGBot/blob/master/readme_pics/queues.png)

To queue up for a game, join one of the channels.

When a queue channel is full, the bot will sort all of the queued players into two optimally-balanced teams, create a voice channel for each team, and move the players from the queue to these channels. It will then configure the TF2 game server. When the server is ready, each player will receive a private message with connection info to the server, along with their assigned team and class.

(Note: for security, the bot randomly generates a new password for the TF2 server at the beginning of each game. 

When the match is completed, each player will also receive the URL for the match's statistics (see example below):

![](https://github.com/nglaze00/TF2PUGBot/blob/master/readme_pics/serverinfo.png)

### First-time configuration
When a new player joins the bot's discord server for the first time, the bot will send them a private message to walk them through configuring their profile in the player database:
![](https://github.com/nglaze00/TF2PUGBot/blob/master/readme_pics/initial_dm.png)
This links each player's Discord and Steam IDs, which lets the bot parse the server's game logs (hosted on **logs.tf**) after each game to update player ratings.

### Class preferences
The player can then set their class preferences using the **!classes** command. Unique preferences are supported for each game format.


#### **Example command usage:**

![](https://github.com/nglaze00/TF2PUGBot/blob/master/readme_pics/classes_cmd.png)

![](https://github.com/nglaze00/TF2PUGBot/blob/master/readme_pics/classes_example.png)

## Team-balancing algorithm
The bot uses an algorithm that ensures that teams are optimally balanced while also respecting each player's class preferences as much as possible. Normally, this algorithm would be trivial (sort all players by Elo, alternate each player's team as Elo increases), but the consideration of class preferences makes it much more complex.

Below is a summary of the algorithm:

1. Choose the unsorted class preferred by the least number of players.
  
   a. Choose the player with the highest Elo that prefers this class.
    
   b. Add this player to the team with the lowest average Elo so far.
    
   c. Repeat until either all players preferring this class have been sorted or no empty spots for this class remain.
    
2. Repeat 1. until all classes have been sorted.
  
3. If any empty spots remain (e.g. 4 spots for a class, but only 3 players prefer it --> 1 empty spot):
  
   a. Choose the highest-Elo unsorted player.
    
   b. If an empty spot remains on the team with lower Elo, sort this player into it.
    
   c. If not (i.e. only one team has an empty spot), sort this player into the first empty spot.
    
   d. Repeat until all players have been sorted.

## Bot setup
To make a new instance of the Discord bot, head over to the [Discord developer portal](https://discordapp.com/developers/applications/). 

Install MySQL on the computer that will run the bot. Within the database you plan to use, create a table *dbName*.players with the following columns: 
 * SteamID64 (varchar(17))
 * discordID (varchar(18))
 * elo (float)
 * wins (int)
 * losses (int)
 * prefs_ulti (varchar(2))
 * prefs_fours (varchar(4))
 * prefs_sixes (varchar(5))

Clone the bot's code and update the following in **PugRunner.java**:

![](https://github.com/nglaze00/TF2PUGBot/blob/master/readme_pics/config_variables.png)
 * Server info: IP address, port, and RCON password (necessary for server configuration)
 * Config file names: **.cfg** files used by TF2 server to set up each game. Using the defaults in the above image is fine, but make sure they're uploaded to your TF2 server (rentals from e.g. serveme.tf have these preloaded)
 * Discord / MySQL: 
     * token: Discord bot token; get this from the Discord developer portal.
     * PUG server ID: Each Discord server has a unique ID; go [here](https://support.discordapp.com/hc/en-us/articles/206346498-Where-can-I-find-my-User-Server-Message-ID-) to learn how to find it.
     * Database name: Name of the MySQL database that will store player data
     
Add the bot to your server and happy playing!
