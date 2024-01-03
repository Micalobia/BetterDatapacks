# Events

Events are similar to advancements in the fact that they trigger when something happens, with a few key differences:

- They won't always be restricted to player actions
- They aren't granted, meaning they can trigger more than once without revoking anything
- They can optionally prevent a vanilla action from happening, such as interacting with an entity
- They're more flexible with how the reward function is executed, changing things such as the execution target/location and (WIP) supplying context-dependent macro arguments, as well as custom ones

### Directory
`data/<namespace>/events`

## Structure
```json
{
  "type": "type:id",
  "conditions": {},
  "data": {},
  "function": "function:id"
}
```
Each type has its own conditions and data object that they use, and in the future they will supply macro arguments into the functions that are run

# Types
## Entity Interactions
Types: `better_datapacks:entity_use` and `better_datapacks:entity_attack`
### Data
```json5
{
  // Whether to cancel the vanilla event, `false` is default
  "cancel": false,
  // Whether to execute as/at the `interactor` (player) or `interactee` (mob interacted with)
  // Interactor (player) by default
  "executor": "interactor"
}
```
### Conditions
All fields are optional and will pass if not present<br>
```json5
{
  // https://minecraft.wiki/w/Advancement/Conditions/entity
  "interactor": {},
  "interactee": {},
  // https://minecraft.wiki/w/Advancement/Conditions/item
  // Checks the item used
  "item": {},
  // Checks the hand used, will use both if not present
  // If both hands pass, this means that it will run the function twice
  "hand": "(mainhand|offhand)"
}
```
## Block Interactions
Types: `better_datapacks:use_block` and `better_datapacks:attack_block`
### Data
`use_block`
```json5
{
  // Whether to cancel the vanilla event, `false` is default
  "cancel": false,
  // Where to execute at, will always execute as the player, default is player
  "at": "(player|block|hit)"
}
```
`attack_block`
```json5
{
  // Whether to cancel the vanilla event, `false` is default
  "cancel": false,
  // Where to execute at, will always execute as the player, default is player
  "at": "(player|block)"
}
```
### Conditions
All fields are optional and will pass if not present<br>
`use_block`
```json5
{
  // https://minecraft.wiki/w/Advancement/Conditions/entity
  "player": {},
  "hit": {
    // https://minecraft.wiki/w/Advancement/Conditions/distance
    // Does not include `absolute` or `horizontal`
    "position": {},
    // A set of sides to check
    // (north|east|south|west|up|down)
    "sides": [],
    // Whether the hit was inside the block or not
    "inside": false
  },
  // https://minecraft.wiki/w/Advancement/Conditions/location
  "location": {}
}
```
`attack_block`
```json5
{
  // https://minecraft.wiki/w/Advancement/Conditions/entity
  "player": {},
  // https://minecraft.wiki/w/Advancement/Conditions/location
  "location": {},
  // https://minecraft.wiki/w/Advancement/Conditions/distance
  // Does not include `absolute` or `horizontal`, is integers instead of doubles
  "position": {},
  // A set of sides to check
  // (north|east|south|west|up|down)
  "sides": [],
}
```