# DenyBack
Deny /back into WorldGuard regions and GriefPrevention claims

Have you ever wanted to stop people from returning to regions or claims with /back? This is the plugin for you! It adds a third-party flag to WorldGuard that you can enable if you don't want players to get back to the region with /back, and a config setting to stop players from returning to untrusted claims.

**Requirements**
WorldGuard plugin is a dependency that you can get [here](https://dev.bukkit.org/projects/worldguard).
GriefPrevention plugin is not required for the plugin to work, but DenyBack will work with it. You can get it [here](https://www.spigotmc.org/resources/griefprevention.1884/).

**Blocked Commands**
The following commands are the default back commands that this plugin will check for:
*/back*
*/eback*
*/return*
*/ereturn*
*/cmi back*
*/cback*

You can add more commands in the config

**Usage**
* Create a region with WorldGuard. More info in the WorldGuard documentation.
* Run the command: "/rg flags" in the region you want to be affected.
* Click the arrows in chat to get to one of the pages. It should look something like this: ![this](https://i.imgur.com/1yU8QXm.png)
* Click allow to stop players from using /back to get into that region
* If you are looking how to deny back into untrusted claims, it is enabled by default. You can turn it off in the config.

**Reload Command**
*/denyback reload* - requires *denyback.admin* to use
