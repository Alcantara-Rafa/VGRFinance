import { useState } from 'react';
import { api } from '../api.js';
import EntradaMarca from '../componentes/EntradaMarca.jsx';
import Mensagem from '../componentes/Mensagem.jsx';

// Cria a conta e já entra: a API abre a sessão e devolve o usuário, como no login.
export default function Cadastro({ aoEntrar, aoIrParaLogin }) {
  const [nome, setNome] = useState('');
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [erro, setErro] = useState(null);
  const [enviando, setEnviando] = useState(false);

  async function cadastrar(evento) {
    evento.preventDefault();
    setEnviando(true);
    setErro(null);
    try {
      aoEntrar(await api.post('/auth/cadastro', { nome, email, senha }));
    } catch (e) {
      setErro(e.message);
    } finally {
      setEnviando(false);
    }
  }

  return (
    <main className="entrada">
      <EntradaMarca />
      <form onSubmit={cadastrar} className="entrada-form">
        <h2>Criar conta</h2>
        <div>
          <label htmlFor="nome">Nome</label>
          <input id="nome" autoComplete="name" required maxLength={100}
                 value={nome} onChange={(e) => setNome(e.target.value)} />
        </div>
        <div>
          <label htmlFor="email">E-mail</label>
          <input id="email" type="email" autoComplete="username" required maxLength={255}
                 value={email} onChange={(e) => setEmail(e.target.value)} />
        </div>
        <div>
          <label htmlFor="senha">Senha</label>
          <input id="senha" type="password" autoComplete="new-password" required minLength={6} maxLength={72}
                 aria-describedby="dica-senha" value={senha} onChange={(e) => setSenha(e.target.value)} />
          <span id="dica-senha" className="secundario">Pelo menos 6 caracteres.</span>
        </div>
        <button className="primario" disabled={enviando}>{enviando ? 'Criando conta…' : 'Criar conta'}</button>
        <Mensagem erro={erro} />
        <p className="entrada-troca">
          Já tem conta?{' '}
          <button type="button" className="link" onClick={aoIrParaLogin}>Entrar</button>
        </p>
      </form>
    </main>
  );
}
