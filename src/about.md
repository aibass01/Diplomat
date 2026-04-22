# Diplomat: a Discord-Based Solution for playing Diplomacy Asynchronously

This project was developed as an Internal Assessment (IA) for the IB Computer Science HL course. 
The client for said IA is a history teacher who runs an after-school club where students play the classic WW1 strategy board game Diplomacy.
After years of students fiddling with slips of paper to submit their orders, the client has requested a digital medium where students can securely submit orders and stay informed about the state of the game.
Enter ***Diplomat***, a Discord bot designed to solve these problems!
%

## Using the Bot

To start a new game with Diplomat, simply type **!new-game**. Diplomat will respond with a message prompting users to claim a country with the **!claim** command.
When you claim a country, Diplomat will prevent other users from claiming that country later and will open a DM with you. This is where you send your orders to Diplomat.
> NOTE: The user who sends the **!new_game** command is saved as the game HOST.

%

### Writing Orders

Players send their orders to Diplomat in their private thread created at the start of the game.
Orders for one turn should all be sent in one message, with the **!orders** command on the first line,
the player's preference for a draw on the second line (either 'draw' or 'no-draw'),
and each order on each of the subsequent lines (1 order per line)
Orders submitted to Diplomat must be written in accordance with the official Diplomacy formatting and abbreviation rules, which are explained below.
For the full Diplomacy rulebook, including all approved abbreviations on page 24, click [here](https://www.hasbro.com/common/instruct/diplomacy.pdf)
* All units default to HOLD if not ordered to do anything else. However, you can also order your units to hold like this:
> A LON H
* Notate move orders like this:
> A MUN - KIE
* Notate support orders like this:
> A RUH S A MUN - KIE
* Notate convoy orders like this:
> F NTH C A EDI - NWY

Players can update their orders before the turn is over by DMing Diplomat a new message with **!orders**.
%

### Revealing Orders

When ready, the game HOST can send the **!reveal** command to have Diplomat analyze everyone's orders and produce a new (tentative) game state.
Then, players will submit their retreat orders, and build/disband orders if it's a fall turn.
The host uses **!reveal_retreats** and **!reveal_builds** to have Diplomat analyze retreat orders and build/disband orders respectively.
%

### Retreat Orders

During gameplay, some units may be DISLODGED and forced to retreat to a nearby territory. 
In this case, Diplomat will inform the player(s) with dislodged units that they need to write retreat orders.
Players send their retreat orders to Diplomat using **!orders**, followed by each required retreat order on it's own following line.
Retreat orders are written as follows:
> A LON > YOR

Where, in this example, a British army dislodged from London retreats to Yorkshire.
If a player fails to submit a retreat order for a unit, their retreat order is illegal, or it conflict with another retreat order, the unit is DISBANDED.
Players may also voluntarily disband dislodged units like so:
> A LON D

%

### Build/Disband Orders

At the end of a fall turn, players must build new units or disband existing units until their total number of units equals the number of supply points the control.
Players can DM Diplomat their build/disband orders using **!orders** like other orders.
Here are some examples:
* If Russia gets to build a unit, i.e. they captured a new supply point this turn, they might send:
> F SEV +
* If Turkey has to disband a unit, i.e. Russia captured their supply point this turn, they might send:
> A ARM D