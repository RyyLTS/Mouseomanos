const $ = (id) => document.getElementById(id);
const form = $('product-form');
let products = [];
let editingId = null;
let deletingId = null;
let busy = false;
const money = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

function notice(message, error = false) {
  $('notice').textContent = message;
  $('notice').className = error ? 'error' : '';
  $('notice').hidden = false;
}

async function request(path = '', options = {}) {
  const response = await fetch('/produtos' + path, options);
  if (!response.ok) throw new Error('Não foi possível concluir a operação. Tente novamente.');
  return response.status === 204 ? null : response.text().then(text => text ? JSON.parse(text) : null);
}

function resetForm() {
  editingId = null;
  form.reset();
  $('form-title').textContent = 'Novo produto';
  $('save').textContent = 'Cadastrar produto';
  $('cancel').hidden = true;
}

function render() {
  const query = $('search').value.trim().toLocaleLowerCase('pt-BR');
  const visible = products.filter(p => `${p.nome ?? ''} ${p.categoria ?? ''}`.toLocaleLowerCase('pt-BR').includes(query));
  $('count').textContent = products.length;
  $('products').replaceChildren();
  $('table').hidden = !visible.length;
  $('empty').hidden = !!visible.length;
  $('empty').querySelector('h3').textContent = products.length ? 'Nenhum resultado encontrado' : 'Seu catálogo começa aqui';
  $('empty').querySelector('p').textContent = products.length ? 'Tente buscar por outro nome ou categoria.' : 'Cadastre seu primeiro produto no formulário ao lado.';
  for (const product of visible) {
    const row = document.createElement('tr');
    const info = document.createElement('td');
    for (const [className, value] of [['product-name', product.nome], ['product-category', product.categoria], ['product-description', product.descricao]]) {
      const text = document.createElement('span');
      text.className = className;
      text.textContent = value ?? '';
      info.append(text);
    }
    const price = document.createElement('td');
    price.className = 'price';
    price.textContent = product.valor == null ? '—' : money.format(product.valor);
    const actions = document.createElement('td');
    const group = document.createElement('div');
    group.className = 'row-actions';
    for (const [label, className, handler] of [
      ['Editar', 'secondary', () => edit(product)],
      ['Excluir', 'delete', () => {
        deletingId = product.id;
        $('delete-message').textContent = `Você está prestes a excluir “${product.nome ?? 'este produto'}”.`;
        $('delete-dialog').showModal();
      }]
    ]) {
      const button = document.createElement('button');
      button.type = 'button';
      button.textContent = label;
      button.className = className;
      button.disabled = busy;
      button.setAttribute('aria-label', `${label} ${product.nome ?? 'produto'}`);
      button.addEventListener('click', handler);
      group.append(button);
    }
    actions.append(group);
    row.append(info, price, actions);
    $('products').append(row);
  }
}

function setBusy(value) {
  busy = value;
  form.querySelectorAll('input, textarea, button').forEach(el => el.disabled = value);
  $('refresh').disabled = value;
  $('confirm-delete').disabled = value;
  $('cancel-delete').disabled = value;
  render();
}

async function load() {
  products = await request();
  render();
}

function edit(product) {
  editingId = product.id;
  for (const field of ['nome', 'categoria', 'descricao', 'valor']) $(field).value = product[field] ?? '';
  $('form-title').textContent = 'Editar produto';
  $('save').textContent = 'Salvar alterações';
  $('cancel').hidden = false;
  $('nome').focus();
  form.scrollIntoView({ behavior: 'smooth', block: 'center' });
}

form.addEventListener('submit', async event => {
  event.preventDefault();
  if (busy) return;
  for (const field of ['nome', 'categoria', 'descricao']) {
    if (!$(field).value.trim()) { $(field).value = ''; $(field).reportValidity(); return; }
  }
  const updating = editingId !== null;
  const body = Object.fromEntries(['nome', 'categoria', 'descricao'].map(field => [field, $(field).value.trim()]));
  body.valor = Number($('valor').value);
  setBusy(true);
  try {
    await request(updating ? `/${editingId}` : '', { method: updating ? 'PUT' : 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
    resetForm();
    notice(updating ? 'Produto atualizado com sucesso.' : 'Produto cadastrado com sucesso.');
    try { await load(); } catch { notice('Produto salvo, mas a lista não pôde ser atualizada. Clique em Atualizar.', true); }
  } catch (error) { notice(error.message, true); }
  finally { setBusy(false); }
});

$('cancel').addEventListener('click', resetForm);
$('search').addEventListener('input', render);
$('refresh').addEventListener('click', async () => {
  setBusy(true);
  try { await load(); $('notice').hidden = true; } catch (error) { notice(error.message, true); }
  finally { setBusy(false); }
});
$('cancel-delete').addEventListener('click', () => $('delete-dialog').close());
$('delete-dialog').addEventListener('cancel', event => { if (busy) event.preventDefault(); });
$('confirm-delete').addEventListener('click', async () => {
  if (busy || deletingId === null) return;
  setBusy(true);
  try {
    await request(`/${deletingId}`, { method: 'DELETE' });
    products = products.filter(p => p.id !== deletingId);
    if (editingId === deletingId) resetForm();
    notice('Produto excluído com sucesso.');
  } catch (error) { notice(error.message, true); }
  finally { deletingId = null; $('delete-dialog').close(); setBusy(false); }
});

setBusy(true);
load().catch(() => notice('Não foi possível carregar os produtos. Verifique o servidor e clique em Atualizar.', true)).finally(() => setBusy(false));
