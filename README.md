# Always Loaded Mod

### **Description**

Server-side fabric mod that allows portal chunk loaders to automatically restart after a server restart.

### **How does it work?**

When the server is about to stop, Always Loaded saves all pending chunk tickets into the file (world/data/chunks.loaded).
Then, when the server starts, it just recreates them.

**Always Loaded** works both in multiplayer and in singleplayer.
