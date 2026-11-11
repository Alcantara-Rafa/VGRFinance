import { useEffect, useState } from 'react';
import { api } from './api.js';
import { mesAtual, mesPorExtenso } from './formato.js';
import Hero from './componentes/Hero.jsx';
import Icone from './componentes/Icone.jsx';
import Login from './paginas/Login.jsx';
import Cadastro from './paginas/Cadastro.jsx';
import Lancamentos from './paginas/Lancamentos.jsx';
import Categorias from './paginas/Categorias.jsx';
import Orcamento from './paginas/Orcamento.jsx';
import Acompanhamento from './paginas/Acompanhamento.jsx';
import Metas from './paginas/Metas.jsx';
import Diagnostico from './paginas/Diagnostico.jsx';

// A primeira página é a que abre depois do login.
const PAGINAS = [
  {
    id: 'acompanhamento', titulo: 'Acompanhamento', componente: Acompanhamento,
    descricao: 'Veja, categoria por categoria, quanto do previsto já foi usado e corrija o rumo antes do fim do mês.',
  },
  {
    id: 'lancamentos', titulo: 'Lançamentos', componente: Lancamentos,
    descricao: 'Registre o que entra e o que sai. Valor, categoria e Enter: o resto já vem preenchido.',
  },
  {
    id: 'orcamento', titulo: 'Orçamento', componente: Orcamento,
    descricao: 'Diga quanto entra no mês e receba quanto destinar a cada categoria. Você ajusta o que quiser.',
  },
  {
    id: 'metas', titulo: 'Metas', componente: Metas,
    descricao: 'Dê um destino à sobra do mês: junte para um objetivo ou defina um teto para uma categoria.',
  },
  {
    id: 'diagnostico', titulo: 'Diagnóstico', componente: Diagnostico,
    descricao: 'Entenda o que os seus números dizem: o que significa cada indicador e qual pode ser o próximo passo.',
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
  // Sem login: mostra a tela de entrar ou a de criar conta.
  const [criandoConta, setCriandoConta] = useState(false);

  useEffect(() => {
    api.get('/auth/eu').then(setUsuario).catch(() => setUsuario(null));
  }, []);

  async function sair() {
    await api.post('/auth/logout');
    setCriandoConta(false);
    setPagina(PAGINAS[0].id);
    setUsuario(null);
  }

  // No Render, a API dorme depois de 15 min sem uso e leva até 1 min para acordar.
  if (usuario === undefined) {
    return (
      <main className="carregando" role="status">
        <div className="carregando-conteudo">
          <span className="marca">VGR<em>Finance</em></span>
          <span className="carregando-barra" aria-hidden="true" />
          <div className="carregando-texto">
            <p>Conectando ao servidor</p>
            <p className="secundario">Pode levar até 1 minuto…</p>
          </div>
        </div>
      </main>
    );
  }
  if (usuario === null) {
    return criandoConta
      ? <Cadastro aoEntrar={setUsuario} aoIrParaLogin={() => setCriandoConta(false)} />
      : <Login aoEntrar={setUsuario} aoIrParaCadastro={() => setCriandoConta(true)} />;
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
            <span className="secundario">{usuario.email}</span>
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
