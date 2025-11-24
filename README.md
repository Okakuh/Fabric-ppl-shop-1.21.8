# PPL Shop Finder

Client-side Minecraft mod for finding shops by sign prices. Helps locate best deals on economy servers.

## Features

- **Smart search**: `/shop <pattern> [stack] [radius]`
- **Regex search**: `/shopr <regex> [stack] [radius]`
- **Price navigation**: Arrow keys to cycle through price groups
- **Visual highlighting**: Green outlines highlight signs in current price group 
- **Smart parsing**: Supports various price formats and units

## Usage

### Basic Commands
```bash
/shop "алмаз"
/shop "кирк+починка" 1
/shop "кирк+починка" 1 50
/shopr "[а-я]+.*алм"
bash```
### Search Patterns
/shop "алмаз+руда"                 # AND search 
/shop "алмаз-уголь"                # OR search  
/shop "кирк+удач-кирка-кирка+шелк" # Mixed (кирк AND удач) or (кирка) or (кирка and шелк)

### Navigation
↑/↓ arrows - Switch between price groups
Backspace - Stop navigation and end search
Current price category shown above hotbar
Signs in current group highlighted with green outlines

## Default Settings
Stack size: 64
Search radius: 40 blocks
Y-range: 0-3 blocks

### Key words for price and amount
"ст" = стак = умножить найденное количество на параметр stack
"ша" = шалкер = умножить найденное количество на параметр stack*27
"м" = мешок = умножить найденное количество на параметр stack
"сло" = слот:
    если найденное количество равно 1, то присвоить количеству значение stack / 4 (для поиска блоков)
    если найденное количество равно 2 и больше, умножить найденное количество на параметр stack (для поиска блоков)

Requirements
Minecraft 1.21+
Fabric Loader
Fabric API
