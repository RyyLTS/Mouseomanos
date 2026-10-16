# Design da X-Squad

O novo visual usa as fotografias de referência fornecidas na conversa como direção: fundo quase preto, destaques vermelhos, títulos altos e condensados, fotografia de comida em destaque, vitrine com navegação por setas e miniaturas e categorias em abas.

Foi criada uma identidade própria para a **X-Squad**, sem usar a marca, a história ou as fotografias do restaurante de referência. O layout abrange a abertura, o cardápio, a seção sobre a marca, o painel lateral do pedido, os cadastros, o histórico, os diálogos e o rodapé. Há adaptação para telas pequenas e respeito à preferência de movimento reduzido.

## Navegação

- **Início:** abertura com fotografia e atalhos.
- **Cardápio:** vitrine de hambúrgueres disponíveis, filtros, busca e adição ao pedido.
- **A X-Squad:** apresentação da marca.
- **Pedir agora / Ver pedido:** painel lateral com quantidades, total e confirmação.
- **Pedidos:** histórico dos pedidos confirmados.
- **Gerenciar cardápio:** acesso junto à busca do cardápio e no rodapé. Permite cadastro, edição, alteração de preços e exclusão.

As fotos da vitrine são ilustrativas e associadas aos tipos de hambúrguer. Novos itens aparecem normalmente na lista; a vitrine acompanha os hambúrgueres disponíveis. Os nomes, preços e descrições são lidos do banco, inclusive na vitrine.

## Arquivos de imagem

Criados com o modo integrado da habilidade **ImageGen**, usando a ferramenta de geração de imagens; sem CLI ou chave de API. As saídas originais foram preservadas e cópias JPEG foram salvas no projeto para uso local, com qualidade 90 e sem recorte.

- [hero-burger.jpg](../src/main/resources/static/assets/hero-burger.jpg): abertura e destaque de hambúrguer duplo.
- [burger-bacon.jpg](../src/main/resources/static/assets/burger-bacon.jpg): destaque de bacon e seção da marca.
- [burger-classic.jpg](../src/main/resources/static/assets/burger-classic.jpg): destaque do hambúrguer clássico.

A fonte Barlow Condensed ExtraBold está em `src/main/resources/static/assets/fonts/`, acompanhada da licença SIL Open Font License. Foi obtida do [repositório oficial Google Fonts](https://github.com/google/fonts/tree/main/ofl/barlowcondensed). Fotos, fonte e estilos são servidos pela aplicação, sem depender de recursos externos ao abrir o site.

## Prompts finais

### Abertura

> Use case: product-mockup. Asset type: full-width restaurant website hero photograph for X-Squad burger restaurant. Create an exceptionally appetizing photorealistic food photograph of one oversized artisan double cheeseburger: glossy toasted brioche, two properly cooked seared beef patties with natural irregular texture, melted yellow cheddar draping over the edges, crisp bacon, subtle caramelized onions, a hint of green lettuce. Dark almost-black studio background, charcoal tabletop, subtle realistic wisps of steam rising, warm dramatic side lighting, golden appetizing highlights and deep rich shadows, premium professional restaurant campaign photography. Wide landscape framing about 16:9. Burger centered horizontally, entire burger visible with breathing room, fills middle 65 percent of image; edges of photograph fade into nearly black. Camera at burger height, shallow depth of field. Realistic proportions, edible ingredients, no exaggerated stacks, no hands, no people, no lettering, no watermark, no logos. This is a photographic asset only, not a webpage screenshot.

### Bacon

> Use case: product-mockup. Asset type: premium burger menu feature photograph for a dark restaurant website. Photorealistic close-up of a single bacon cheeseburger on a charcoal slate surface. Toasted glossy brioche, one thick seared 160 g beef patty, melted cheddar, crisp curled strips of bacon, a small amount of house mayonnaise and caramelized onion, no lettuce. Three-quarter front view, entire burger visible, centered with ample space around it, wide landscape composition about 3:2. Very dark almost-black warm studio background, warm directional food photography lighting, sharp natural appetizing beef and bacon textures, subtle background bokeh, elegant restaurant advertising photograph. No words, no logos, no people, no hands, no artificial plastic appearance, no watermark.

### Clássico

> Use case: product-mockup. Asset type: premium burger menu feature photograph for a dark restaurant website. Photorealistic classic Brazilian artisan cheeseburger: toasted golden brioche, one seared thick beef patty, melted cheddar slice, fresh green lettuce and red tomato, small amount of house sauce. Whole burger visible centered on a dark charcoal slate, camera at food level in a slight three-quarter view. Wide landscape composition about 3:2 with dark negative space around the burger, moody warm studio lighting and appetizing rim light. Professional restaurant product photography with realistic bread, juicy beef and fresh vegetable textures. Consistent dark background and golden lighting. No text, no logo, no people, no hands, no watermark.
