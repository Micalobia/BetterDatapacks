# Main command
`execute raycast <distance> ...`

### Subcommands
```
execute raycast <distance> at (*|<predicate>)
execute raycast <distance> as (*|<predicate>)
execute raycast <distance> positioned at (*|<predicate>) (entity|hit) 
execute raycast <distance> block (*|<block>) (collider|outline|visual) (none|source|any)
execute (if|unless) raycast <distance> block (exists|<block>) (collider|outline|visual) (none|source|any)
execute (if|unless) raycast <distance> entity (exists|<predicate>)
```


## At
`at (*|<predicate>)`
<br>Sets the execution position and rotation to match those of first matched entity; does not change executor.
<br>**Fails** if no entity is hit.

## As
`as (*|<predicate>)`
<br>Sets the executor to first matched entity, without changing execution position, rotation, and anchor.
<br>**ails** if no entity is hit.

## Positioned At
`positioned at (*|<predicate>) (entity|hit) `
<br>Sets the execution position to match that of first matched entity; does not change executor, or rotation.
<br>`(entity|hit)` is either the entity's position, or where the ray hit it.
<br>**Fails** if no entity is hit.

## Block
`block (*|<block>) (collider|outline|visual) (none|source|any|water)`
<br>Sets the execution position to the hit location on the first matching block, or the end of the ray.
<br>`(collider|outline|visual)` is the collision mode.
<br>`(none|source|any)` is the fluid handling mode.

## If Block
`... block (*|<block>) (collider|outline|visual) (none|source|any|water)`
<br>Compares the first block hit by the ray to the given block ID.
<br>`(collider|outline|visual)` is the collision mode.
<br>`(none|source|any)` is the fluid handling mode.
<br>**Passes** if the block matches.
<br>**Fails** if the block does not match, or no block is hit.

## If Entity
`... entity (*|<predicate>)`
<br>Compares every entity hit by the ray to the given predicate.
<br>**Passes** if any entity matches, **does not return the count**
<br>**Fails** if no entities match.