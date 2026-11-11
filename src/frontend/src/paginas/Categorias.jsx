import { useEffect, useState } from 'react';
import { api } from '../api.js';
import Mensagem from '../componentes/Mensagem.jsx';

const VAZIA = { nome: '', essencial: false };

export default function Categorias() {
  const [categorias, setCategorias] = useState([]);
  const [form, setForm] = useState(VAZIA);
  const [editando, setEditando] = useState(null);
  const [erro, setErro] = useState(null);

  const carregar = () => api.get('/categorias').then(setCategorias).catch((e) => setErro(e.message));

  useEffect(() => {
    carregar();
  }, []);

  async function salvar(evento) {
    evento.preventDefault();
    setErro(null);
    try {
      if (editando) {
        await api.put(`/categorias/${editando}`, form);
      } else {
        await api.post('/categorias', form);
      }
      cancelar();
      await carregar();
    } catch (e) {
      setErro(e.message);
    }
  }

  function editar(categoria) {
    setEditando(categoria.id);
    setForm({ nome: categoria.nome, essencial: categoria.essencial });
  }

  function cancelar() {
    setEditando(null);
    setForm(VAZIA);
  }

  async function excluir(categoria) {
    setErro(null);
    try {
      await api.excluir(`/categorias/${categoria.id}`);
      await carregar();
    } catch (e) {
      setErro(e.message);
    }
  }

  return (
    <>
      <form className="cartao" onSubmit={salvar}>
        <h2>{editando ? 'Alterar categoria' : 'Nova categoria'}</h2>
        <div className="linha-form">
          <div>
            <label htmlFor="nome-categoria">Nome</label>
            <input id="nome-categoria" required maxLength={60} value={form.nome}
                   onChange={(e) => setForm({ ...form, nome: e.target.value })} />
          </div>
          <label className="check">
            <input type="checkbox" checked={form.essencial}
                   onChange={(e) => setForm({ ...form, essencial: e.target.checked })} />
            Essencial (não dá para cortar sem prejuízo)
          </label>
          <button className="primario">{editando ? 'Salvar alterações' : 'Criar'}</button>
          {editando && <button type="button" onClick={cancelar}>Cancelar</button>}
        </div>
        <Mensagem erro={erro} />
      </form>

      <section className="cartao">
        {categorias.length === 0 ? (
          <p className="vazio">
            Você ainda não tem categorias. Comece por Moradia, Alimentação e Transporte,
            depois marque quais são essenciais.
          </p>
        ) : (
          <ul className="lista">
            {categorias.map((c) => (
              <li key={c.id}>
                <span className="principal">
                  {c.nome}
                  {c.essencial && <span className="selo">essencial</span>}
                </span>
                <span className="acoes">
                  <button className="discreto" onClick={() => editar(c)}>Alterar</button>
                  <button className="discreto" onClick={() => excluir(c)}>Excluir</button>
                </span>
              </li>
            ))}
          </ul>
        )}
      </section>
    </>
  );
}
