# Block Broken Advancements
Adds the `better_datapacks:block_broken` trigger, with the following additional parameters:

```json
{
  "trigger": "better_datapacks:block_broken",
  "conditions": {
    "block": {
      "blocks": [
        "list",
        "of",
        "block",
        "ids"
      ],
      "tag": "The block tag",
      "nbt": "The block nbt",
      "state": {
        "key": "value",
        "key_with_range": {
          "min": "minimum",
          "max": "maximum"
        }
      }
    }
  }
}
```

Everything in `conditions.block` is optional, so the simplist trigger you could make looks as follows:
```json
{
  "trigger": "better_datapacks:block_broken",
  "conditions": {
    "block": {}
  }
}
```