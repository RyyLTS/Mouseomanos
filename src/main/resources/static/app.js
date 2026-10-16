const $ = id => document.getElementById(id);
const money = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const dateFormat = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
const cart = new Map();
let products = [], menu = [], orders = [];
let editingId = null, deletingId = null, priceId = null;
let busy = false, loaded = false, synchronized = false, loadFailed = false;
let refreshPromise = null;
let featuredId = null;
let noticeTimer;

function element(tag, className, text) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text !== undefined) node.textContent = text;
  return node;
}

function button(label, className, action, accessibleName = label) {
  const node = element('button', className, label);
  node.type = 'button';
  node.disabled = busy;
  node.setAttribute('aria-label', accessibleName);
  node.addEventListener('click', action);
  return node;
}

function notice(message, error = false) {
  clearTimeout(noticeTimer);
  $('notice').textContent = message;
  $('notice').className = error ? 'error' : '';
  $('notice').hidden = false;
  if ($('cart-dialog').open) {
    $('cart-notice').textContent = message;
    $('cart-notice').className = error ? 'drawer-notice error' : 'drawer-notice';
    $('cart-notice').hidden = false;
  }
  if (!error) noticeTimer = setTimeout(() => { $('notice').hidden = true; }, 6500);
}

async function request(path, options = {}) {
  const response = await fetch(path, { ...options, headers: { 'Content-Type': 'application/json', ...options.headers } });
  const text = await response.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch { /* Non-JSON server failures use the fallback below. */ }
  if (!response.ok) throw new Error(data?.message || 'Não foi possível concluir a operação. Atualize e tente novamente.');
  return data;
}

function navigate(view, section) {
  const titles = {
    menu: ['Nosso cardápio', 'Escolha seu próximo favorito.'],
    admin: ['Gerenciar cardápio', 'Cadastre, edite e exclua os itens da X-Squad. As alterações aparecem no cardápio.'],
    orders: ['Pedidos', 'Todos os pedidos confirmados, com os valores registrados na compra.']
  };
  if ($('cart-dialog').open) $('cart-dialog').close();
  for (const name of Object.keys(titles)) $('view-' + name).hidden = name !== view;
  $('workspace-head').hidden = view === 'menu';
  document.querySelectorAll('nav [data-view]').forEach(node => {
    if (node.dataset.view === view && (!node.dataset.section || node.dataset.section === (section || 'inicio'))) node.setAttribute('aria-current', 'page');
    else node.removeAttribute('aria-current');
  });
  [$('page-title').textContent, $('page-description').textContent] = titles[view];
  renderCart();
  const target = view === 'menu' ? $(section || 'inicio') : $('workspace-head');
  target.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function openCart() {
  if ($('cart-dialog').open) return;
  $('cart-notice').hidden = true;
  $('cart-dialog').showModal();
  renderCart();
}

function closeCart() {
  if (busy) return;
  $('cart-dialog').close();
  renderCart();
}

function normalized(value) { return value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase(); }

function menuCategories() {
  const order = ['hamburgueres', 'burgers', 'combos', 'acompanhamentos', 'bebidas', 'sobremesas'];
  const rank = value => { const index = order.indexOf(normalized(value)); return index < 0 ? order.length : index; };
  return [...new Set(menu.map(p => p.categoria))].sort((a, b) => rank(a) - rank(b) || a.localeCompare(b, 'pt-BR'));
}

function featuredProducts() {
  return menu.filter(p => /hamb|burger/.test(normalized(p.categoria)) || normalized(p.nome).startsWith('x-')).slice(0, 6);
}

function productImage(product) {
  const name = normalized(product.nome);
  return name.includes('bacon') ? '/assets/burger-bacon.jpg' : name.includes('duplo') ? '/assets/hero-burger.jpg' : '/assets/burger-classic.jpg';
}

function renderFeatured() {
  const items = featuredProducts();
  $('featured').hidden = !items.length;
  if (!items.length) { featuredId = null; return; }
  const selected = items.find(item => item.id === featuredId) || items[0];
  featuredId = selected.id;
  const photo = $('featured-image');
  if (photo.getAttribute('src') !== productImage(selected)) photo.src = productImage(selected);
  photo.alt = `Fotografia ilustrativa de ${selected.nome}`;
  $('featured-category').textContent = selected.categoria;
  $('featured-name').textContent = selected.nome;
  $('featured-description').textContent = selected.descricao;
  $('featured-price').textContent = money.format(selected.valor);
  $('featured-position').textContent = `${String(items.indexOf(selected) + 1).padStart(2, '0')} / ${String(items.length).padStart(2, '0')}`;
  $('featured-add').disabled = busy || !synchronized || (cart.get(selected.id)?.quantidade || 0) >= 99;
  $('featured-add').setAttribute('aria-label', `Adicionar ${selected.nome} em destaque ao pedido`);
  $('featured-prev').hidden = $('featured-next').hidden = items.length < 2;
  $('featured-thumbnails').replaceChildren();
  for (const item of items) {
    const thumbnail = button('', '', () => { featuredId = item.id; renderFeatured(); }, `Ver destaque ${item.nome}`);
    thumbnail.setAttribute('aria-pressed', String(item.id === selected.id));
    const image = element('img');
    image.src = productImage(item);
    image.alt = '';
    image.loading = 'lazy';
    thumbnail.append(image);
    $('featured-thumbnails').append(thumbnail);
  }
}

function moveFeatured(delta) {
  const items = featuredProducts();
  if (!items.length) return;
  const index = Math.max(0, items.findIndex(item => item.id === featuredId));
  featuredId = items[(index + delta + items.length) % items.length].id;
  renderFeatured();
}

function syncCart(nextMenu) {
  let changed = false;
  for (const [id, item] of cart) {
    const current = nextMenu.find(product => product.id === id);
    if (!current) { cart.delete(id); changed = true; continue; }
    if (Math.round(item.valor * 100) !== Math.round(current.valor * 100)) changed = true;
    cart.set(id, { ...current, quantidade: item.quantidade });
  }
  return changed;
}

async function load(warn = true, fresh = false) {
  if (refreshPromise) {
    if (!fresh) return refreshPromise;
    // A refresh that started before a write cannot confirm the result of that write.
    await refreshPromise.catch(() => {});
    return load(warn);
  }
  refreshPromise = (async () => {
    try {
      const data = await Promise.all([request('/produtos'), request('/cardapio'), request('/pedidos')]);
      const changed = syncCart(data[1]);
      [products, menu, orders] = data;
      loaded = synchronized = true;
      loadFailed = false;
      render();
      if (changed && warn) notice('O cardápio mudou. Atualizamos os preços e removemos do seu pedido os itens indisponíveis. Confira o total antes de confirmar.');
      return changed;
    } catch (error) {
      synchronized = false;
      loadFailed = true;
      render();
      throw error;
    }
  })().finally(() => { refreshPromise = null; });
  return refreshPromise;
}

function renderMenu() {
  const selected = $('category-filter').value;
  const categories = menuCategories();
  $('category-filter').replaceChildren(new Option('Todas as categorias', ''));
  for (const category of categories) {
    $('category-filter').append(new Option(category, category));
  }
  $('category-filter').value = [...$('category-filter').options].some(o => o.value === selected) ? selected : '';
  const query = $('menu-search').value.trim().toLocaleLowerCase('pt-BR');
  const category = $('category-filter').value;
  $('category-tabs').replaceChildren();
  for (const value of ['', ...categories]) {
    const tab = button(value || 'Todos', '', () => { $('category-filter').value = value; renderMenu(); });
    tab.setAttribute('aria-pressed', String(value === category));
    $('category-tabs').append(tab);
  }
  const visible = menu.filter(p => (!category || p.categoria === category) && `${p.nome} ${p.categoria} ${p.descricao}`.toLocaleLowerCase('pt-BR').includes(query))
    .sort((a, b) => categories.indexOf(a.categoria) - categories.indexOf(b.categoria) || a.nome.localeCompare(b.nome, 'pt-BR'));
  $('menu-count').textContent = menu.length;
  $('menu-items').replaceChildren();
  $('menu-empty').hidden = !!visible.length;
  const heading = $('menu-empty').querySelector('h3');
  const description = $('menu-empty').querySelector('p');
  if (!loaded) {
    heading.textContent = loadFailed ? 'Não foi possível carregar o cardápio' : 'Carregando cardápio…';
    description.textContent = loadFailed ? 'Verifique a conexão e clique em Atualizar cardápio.' : 'Aguarde um instante.';
  } else {
    heading.textContent = menu.length ? 'Nenhum item encontrado' : 'O cardápio está sendo preparado';
    description.textContent = menu.length ? 'Tente outra busca ou categoria.' : 'Cadastre e disponibilize itens em Gerenciar cardápio para começar.';
  }
  for (const product of visible) {
    const card = element('article', 'card menu-card');
    card.append(element('span', 'category', product.categoria), element('h3', '', product.nome), element('p', '', product.descricao));
    const bottom = element('div', 'menu-bottom');
    const add = button('Adicionar +', '', () => addToCart(product.id), `Adicionar ${product.nome} ao pedido`);
    add.disabled = busy || !synchronized || (cart.get(product.id)?.quantidade || 0) >= 99;
    bottom.append(element('strong', '', money.format(product.valor)), add);
    card.append(bottom);
    $('menu-items').append(card);
  }
  renderFeatured();
}

function renderProducts() {
  const query = $('search').value.trim().toLocaleLowerCase('pt-BR');
  const visible = products.filter(p => `${p.nome} ${p.categoria}`.toLocaleLowerCase('pt-BR').includes(query));
  $('count').textContent = products.length;
  $('products').replaceChildren();
  $('table').hidden = !visible.length;
  $('empty').hidden = !!visible.length;
  $('empty').querySelector('h3').textContent = products.length ? 'Nenhum resultado encontrado' : 'Nenhum item cadastrado';
  $('empty').querySelector('p').textContent = products.length ? 'Tente outro nome ou categoria.' : 'Cadastre o primeiro item no formulário.';
  for (const product of visible) {
    const row = element('tr');
    const info = element('td');
    for (const [className, value] of [['product-name', product.nome], ['product-category', product.categoria], ['product-description', product.descricao]]) {
      info.append(element('span', className, value));
    }
    info.append(element('span', product.disponivel ? 'availability' : 'availability off', product.disponivel ? '● Disponível' : '● Indisponível'));
    const actions = element('td');
    const group = element('div', 'row-actions');
    group.append(
      button('Alterar preço', 'secondary', () => openPrice(product), `Alterar preço de ${product.nome}`),
      button('Editar', 'secondary', () => edit(product), `Editar ${product.nome}`),
      button(product.disponivel ? 'Indisponibilizar' : 'Disponibilizar', 'secondary', () => mutate(
        () => request(`/produtos/${product.id}/disponibilidade`, { method: 'PATCH', body: JSON.stringify({ disponivel: !product.disponivel }) }),
        product.disponivel ? 'Item retirado dos disponíveis.' : 'Item disponível no cardápio.'
      ), `${product.disponivel ? 'Indisponibilizar' : 'Disponibilizar'} ${product.nome}`),
      button('Excluir', 'delete', () => {
        deletingId = product.id;
        $('delete-message').textContent = `Excluir “${product.nome}” do cardápio?`;
        $('delete-dialog').showModal();
      }, `Excluir ${product.nome}`)
    );
    actions.append(group);
    row.append(info, element('td', 'price', money.format(product.valor)), actions);
    $('products').append(row);
  }
}

function renderCart() {
  $('cart-items').replaceChildren();
  $('cart-empty').hidden = !!cart.size;
  let quantity = 0, totalCents = 0;
  for (const item of cart.values()) {
    quantity += item.quantidade;
    totalCents += Math.round(item.valor * 100) * item.quantidade;
    const line = element('li', 'cart-line');
    const top = element('div', 'cart-line-top');
    top.append(element('strong', '', item.nome), element('span', '', money.format(Math.round(item.valor * 100) * item.quantidade / 100)));
    const controls = element('div', 'quantity');
    const minus = button('−', 'secondary', () => changeQuantity(item.id, -1), `Diminuir quantidade de ${item.nome}`);
    const plus = button('+', 'secondary', () => changeQuantity(item.id, 1), `Aumentar quantidade de ${item.nome}`);
    plus.disabled = busy || item.quantidade >= 99;
    const count = element('output', '', item.quantidade);
    count.setAttribute('aria-label', `Quantidade de ${item.nome}`);
    controls.append(minus, count, plus, button('Remover', 'remove-item', () => { cart.delete(item.id); renderCart(); renderMenu(); }, `Remover ${item.nome} do pedido`));
    line.append(top, controls);
    $('cart-items').append(line);
  }
  $('cart-count').textContent = quantity;
  $('cart-badge').textContent = quantity;
  $('floating-count').textContent = quantity;
  $('floating-total').textContent = money.format(totalCents / 100);
  $('floating-cart').hidden = !cart.size || $('cart-dialog').open;
  $('cart-total').textContent = money.format(totalCents / 100);
  $('place-order').disabled = busy || !cart.size || !synchronized;
}

function renderOrders() {
  $('order-count').textContent = orders.length;
  $('orders-empty').hidden = !!orders.length;
  $('orders-list').replaceChildren();
  for (const order of orders) {
    const card = element('article', 'card');
    const heading = element('div', 'order-heading');
    heading.append(element('h3', '', `Pedido #${order.id}`), element('span', 'status', 'CONFIRMADO'));
    card.append(heading, element('p', 'order-meta', `${order.cliente} · ${dateFormat.format(new Date(order.criadoEm))}`));
    const items = element('ul', 'receipt-items');
    for (const item of order.itens) {
      const line = element('li');
      const info = element('div', '', `${item.quantidade} × ${item.nome}`);
      info.append(element('small', '', `${money.format(item.precoUnitario)} por unidade`));
      line.append(info, element('strong', '', money.format(item.subtotal)));
      items.append(line);
    }
    card.append(items);
    if (order.observacao) card.append(element('p', 'order-observation', `Observação: ${order.observacao}`));
    const total = element('div', 'receipt-total');
    total.append(element('span', '', 'Total do pedido'), element('strong', '', money.format(order.total)));
    card.append(total);
    $('orders-list').append(card);
  }
}

function render() { renderMenu(); renderProducts(); renderCart(); renderOrders(); }

function setBusy(value) {
  busy = value;
  document.querySelectorAll('#product-form input, #product-form textarea, #product-form button, #order-form input, #order-form textarea, #price-form input, #price-form button').forEach(node => node.disabled = value);
  for (const id of ['refresh', 'refresh-menu', 'refresh-orders', 'confirm-delete', 'cancel-delete']) $(id).disabled = value;
  $('close-cart').disabled = $('continue-shopping').disabled = value;
  render();
}

async function mutate(action, message) {
  if (busy) return;
  setBusy(true);
  let committed = false;
  try {
    await action();
    committed = true;
    const changed = await load(false, true);
    notice(message + (changed ? ' Confira os itens e o novo total do seu pedido.' : ''));
  } catch (error) {
    notice(committed ? 'Alteração salva. Não foi possível atualizar a tela; clique em Atualizar.' : error.message, true);
  } finally { setBusy(false); }
}

function resetForm() {
  editingId = null;
  $('product-form').reset();
  $('form-title').textContent = 'Cadastrar item';
  $('save').textContent = 'Cadastrar item';
  $('cancel').hidden = true;
}

function edit(product) {
  editingId = product.id;
  for (const field of ['nome', 'categoria', 'descricao', 'valor']) $(field).value = product[field];
  $('disponivel').checked = product.disponivel;
  $('form-title').textContent = 'Editar item';
  $('save').textContent = 'Salvar alterações';
  $('cancel').hidden = false;
  $('nome').focus();
  $('product-form').scrollIntoView({ behavior: 'smooth', block: 'center' });
}

function openPrice(product) {
  priceId = product.id;
  $('price-product').textContent = product.nome;
  $('new-price').value = product.valor;
  $('price-error').hidden = true;
  $('price-dialog').showModal();
}

function addToCart(id) {
  if (busy || !synchronized) return;
  const product = menu.find(p => p.id === id);
  if (!product) return;
  const quantity = (cart.get(id)?.quantidade || 0) + 1;
  if (quantity > 99) return;
  cart.set(id, { ...product, quantidade: quantity });
  $('order-confirmation').hidden = true;
  renderCart(); renderMenu();
  notice(`${product.nome} adicionado ao pedido.`);
}

function changeQuantity(id, delta) {
  if (busy) return;
  const item = cart.get(id);
  if (!item) return;
  item.quantidade = Math.min(99, item.quantidade + delta);
  if (item.quantidade <= 0) cart.delete(id);
  renderCart(); renderMenu();
}

document.querySelectorAll('[data-view]').forEach(node => node.addEventListener('click', () => {
  navigate(node.dataset.view, node.dataset.section);
  if (!busy) load().catch(() => notice('Não foi possível atualizar os dados. Verifique a conexão e clique em Atualizar.', true));
}));
$('featured-prev').addEventListener('click', () => moveFeatured(-1));
$('featured-next').addEventListener('click', () => moveFeatured(1));
$('featured-add').addEventListener('click', () => addToCart(featuredId));
$('open-cart').addEventListener('click', openCart);
$('floating-cart').addEventListener('click', openCart);
$('close-cart').addEventListener('click', closeCart);
$('continue-shopping').addEventListener('click', () => { closeCart(); navigate('menu', 'cardapio'); });
$('cart-dialog').addEventListener('cancel', event => { if (busy) event.preventDefault(); });
$('cart-dialog').addEventListener('close', renderCart);
$('menu-search').addEventListener('input', renderMenu);
$('category-filter').addEventListener('change', renderMenu);
$('search').addEventListener('input', renderProducts);
$('cancel').addEventListener('click', resetForm);

$('product-form').addEventListener('submit', event => {
  event.preventDefault();
  if (busy) return;
  for (const field of ['nome', 'categoria', 'descricao']) {
    if (!$(field).value.trim()) { $(field).value = ''; $(field).reportValidity(); return; }
  }
  const id = editingId;
  const body = Object.fromEntries(['nome', 'categoria', 'descricao'].map(field => [field, $(field).value.trim()]));
  body.valor = Number($('valor').value);
  body.disponivel = $('disponivel').checked;
  mutate(async () => {
    await request(id === null ? '/produtos' : `/produtos/${id}`, { method: id === null ? 'POST' : 'PUT', body: JSON.stringify(body) });
    resetForm();
  }, id === null ? 'Item cadastrado com sucesso.' : 'Item atualizado com sucesso.');
});

$('price-form').addEventListener('submit', async event => {
  event.preventDefault();
  if (busy || priceId === null) return;
  const valor = Number($('new-price').value);
  setBusy(true);
  let committed = false;
  try {
    await request(`/produtos/${priceId}/preco`, { method: 'PATCH', body: JSON.stringify({ valor }) });
    if (editingId === priceId) $('valor').value = valor;
    committed = true;
    $('price-dialog').close();
    const changed = await load(false, true);
    notice('Preço atualizado no cardápio.' + (changed ? ' Confira o novo total do seu pedido.' : ''));
  } catch (error) {
    if (committed) notice('Preço salvo. Clique em Atualizar para recarregar a tela.', true);
    else { $('price-error').textContent = error.message; $('price-error').hidden = false; }
  } finally { setBusy(false); }
});
$('cancel-price').addEventListener('click', () => $('price-dialog').close());
$('cancel-delete').addEventListener('click', () => $('delete-dialog').close());
for (const id of ['delete-dialog', 'price-dialog']) $(id).addEventListener('cancel', event => { if (busy) event.preventDefault(); });
$('confirm-delete').addEventListener('click', () => {
  if (busy || deletingId === null) return;
  const id = deletingId;
  mutate(async () => {
    await request(`/produtos/${id}`, { method: 'DELETE' });
    if (editingId === id) resetForm();
    deletingId = null;
    $('delete-dialog').close();
  }, 'Item excluído do cardápio.');
});

for (const id of ['refresh', 'refresh-menu', 'refresh-orders']) $(id).addEventListener('click', async () => {
  if (busy) return;
  setBusy(true);
  try { const changed = await load(); if (!changed) notice('Dados atualizados.'); }
  catch { notice('Não foi possível atualizar. Verifique a conexão e tente novamente.', true); }
  finally { setBusy(false); }
});

$('order-form').addEventListener('submit', async event => {
  event.preventDefault();
  if (busy || !cart.size) return;
  const cliente = $('cliente').value.trim();
  if (!cliente) { $('cliente').value = ''; $('cliente').reportValidity(); return; }
  setBusy(true);
  try {
    if (await load(false, true)) { notice('O cardápio mudou. Confira os itens e o novo total antes de confirmar novamente.', true); return; }
    if (!cart.size) { notice('Adicione ao menos um item disponível ao pedido.', true); return; }
    const created = await request('/pedidos', { method: 'POST', body: JSON.stringify({
      cliente, observacao: $('observacao').value.trim(),
      itens: [...cart.values()].map(item => ({ produtoId: item.id, quantidade: item.quantidade, precoUnitario: item.valor }))
    }) });
    cart.clear();
    $('order-form').reset();
    const receipt = $('order-confirmation');
    receipt.replaceChildren(element('strong', '', `Pedido #${created.id} confirmado!`), element('span', '', `${created.cliente} · Total ${money.format(created.total)}`), button('Ver pedido confirmado', '', () => navigate('orders')));
    // The confirmation button remains usable after the submission completes.
    receipt.querySelector('button').disabled = false;
    receipt.hidden = false;
    notice(`Pedido #${created.id} criado com sucesso.`);
    try { await load(false, true); }
    catch { orders = [created, ...orders.filter(order => order.id !== created.id)]; notice(`Pedido #${created.id} confirmado. A lista não pôde ser atualizada; clique em Atualizar.`, true); }
  } catch (error) {
    notice(`${error.message} Se houve falha de conexão, confira a tela Pedidos antes de tentar novamente.`, true);
    try { await load(false); } catch { /* Keep the original error and cart for review. */ }
  } finally { setBusy(false); }
});

setBusy(true);
load().catch(() => notice('Não foi possível carregar os dados. Verifique o servidor e clique em Atualizar.', true)).finally(() => setBusy(false));
setInterval(() => {
  if (!busy && document.visibilityState === 'visible') load().catch(() => notice('A atualização automática falhou. Verifique a conexão e clique em Atualizar.', true));
}, 10000);
document.addEventListener('visibilitychange', () => {
  if (document.visibilityState === 'visible' && !busy) load().catch(() => notice('Não foi possível atualizar os dados.', true));
});
