# Blackjack LibGDX Edition - Assets

This directory contains game assets for the LibGDX Blackjack game.

## Required Assets

### Card Textures
Place card images in the `cards/` subdirectory with the following naming convention:

```
cards/
  hearts_ace.png
  hearts_2.png
  hearts_3.png
  ...
  hearts_king.png
  diamonds_ace.png
  diamonds_2.png
  ...
  clubs_ace.png
  ...
  spades_king.png
  back.png (card back image)
```

### UI Skin (Optional)
Place a LibGDX UI skin in the `skin/` subdirectory:

```
skin/
  uiskin.json
  uiskin.atlas
  uiskin.png
```

You can download the default LibGDX UI skin from:
https://github.com/libgdx/libgdx/tree/master/tests/gdx-tests-android/assets/data

### Table Background (Optional)
Place a table background image:

```
table/
  table_bg.png
```

## Free Card Image Resources

You can find free playing card images at:
- https://opengameart.org/content/playing-cards
- https://opengameart.org/content/playing-cards-0
- https://itch.io/game-assets/free/tag-cards

## Asset Loading

The game will work even without texture assets - it will display placeholder text-based cards.
However, for the best visual experience, add the card textures as described above.

## Creating Your Own Cards

If you want to create your own card images:
- Recommended size: 200x300 pixels (will be scaled automatically)
- Format: PNG with transparency
- Keep consistent styling across all cards