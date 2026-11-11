import { useEffect, useState } from 'react';
import { api } from './api.js';
import { mesAtual, mesPorExtenso } from './formato.js';
import Hero from './componentes/Hero.jsx';
import Icone from './componentes/Icone.jsx';
import Login from './paginas/Login.jsx';
import Lancamentos from './paginas/Lancamentos.jsx';
import Categorias from './paginas/Categorias.jsx';
import Orcamento from './paginas/Orcamento.jsx';

const PAGINAS = [
  {
    id: 'lancamentos', titulo: 'Lançamentos', componente: Lancamentos,
    descricao: 'Registre o que entra e o que sai. Valor, categoria e Enter: o resto já vem preenchido.',
  },
  {
    id: 'orcamento', titulo: 'Orçamento', componente: Orcamento,
    descricao: 'Diga quanto entra no mês e receba quanto destinar a cada categoria. Você ajusta o que quiser.',
  },
  {
    id: 'categorias', titulo: 'Categorias', componente: Categorias,
    descricao: 'Organize seus gastos e marque o que é essencial: é a base da divisão do orçamento.',
  },
];

export default function App() {
  // undefined = ainda verificando a sessão; null = sem login.
  const [usuario, setUsuario] = useState(undefined);
  const [pagina, setPagina] = useState(PAGINAS[0].id);

  useEffect(() => {
    api.get('/auth/eu').then(setUsuario).catch(() => setUsuario(null));
  }, []);

  async function sair() {
    await api.post('/auth/logout');
    setUsuario(null);
  }

  if (usuario === undefined) {
    return null;
  }
  if (usuario === null) {
    return <Login aoEntrar={setUsuario} />;
  }

  const atual = PAGINAS.find((p) => p.id === pagina);
  const Conteudo = atual.componente;
  const primeiroNome = usuario.nome.split(' ')[0];

  return (
    <div className="aplicacao">
      <aside className="lateral">
        <span className="marca">VGR<em>Finance</em></span>
        <nav className="navegacao" aria-label="Principal">
          {PAGINAS.map((p) => (
            <button key={p.id} aria-current={p.id === pagina ? 'page' : undefined} onClick={() => setPagina(p.id)}>
              <Icone nome={p.id} />
              {p.titulo}
            </button>
          ))}
        </nav>
        <div className="perfil">
          <span className="avatar" aria-hidden="true">{primeiroNome[0]}</span>
          <span className="perfil-nome">
            {usuario.nome}
            <span className="secundario">{usuario.papel === 'ADMIN' ? 'Administrador' : 'Usuário'}</span>
          </span>
          <button className="discreto" onClick={sair} aria-label="Sair">
            <Icone nome="sair" />
          </button>
        </div>
      </aside>
      <main className="conteudo">
        <Hero saudacao={`Olá, ${primeiroNome} · ${mesPorExtenso(mesAtual())}`}
              titulo={atual.titulo} descricao={atual.descricao} />
        <Conteudo aoIrPara={setPagina} />
      </main>
    </div>
  );
}
