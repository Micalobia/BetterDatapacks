# Better Datapacks

![Environment: Server](https://img.shields.io/badge/environment-server-orangered?style=flat-square)
![Mod Loader: Fabric](https://img.shields.io/badge/modloader-fabric-informational?style=flat-square)
<br>

This mod aims to improve and add upon what is possible with the datapack system in Vanilla, ideally shooting for
completely server-side features

[![Requires Fabric API](https://i.imgur.com/Ol1Tcf8t.png)](https://modrinth.com/mod/fabric-api)

## Implemented

- `execute raycast` and `execute if raycast` have been added to the subcommands. More about it [here](Raycast.md)
- Advancement trigger `better_datapacks:block_broken` detects when a block is broken. More about
  it [here](BlockBroken.md)
- Recipe type `better_datapacks:empty` lets you overwrite a recipe with nothing, without an error log.
- Recipe type `better_datapacks:potion` and `better_datapacks:potion_item` to make potion recipes
- `calculate` command that lets you do a lot of math operations between just about any number and storing it just about anywhere
- Nbt Crafting in shaped, shapeless, and smithing recipes
- Events, which go in the `events` folder in a datapack, similar to advancements. More about it [here](Events.md)

## Planned

- Negative Tags (removing something from a tag without rewriting the entire thing)
- Custom Enchants* (primitive, will do nothing but be compatible with anvils and such)
- Custom Commands (with arguments and everything, will execute a function using the macro feature)

*might not be able to do this only serverside

## Feedback

If you find a bug or have an idea, be sure to create an issue!
Would also love if anyone wants to make a pull request, help is *always* appreciated!