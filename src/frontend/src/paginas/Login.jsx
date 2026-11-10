import { useState } from 'react';
import { api } from '../api.js';
import Mensagem from '../componentes/Mensagem.jsx';

export default function Login({ aoEntrar }) {
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [erro, setErro] = useState(null);
  const [enviando, setEnviando] = useState(false);

  async function entrar(evento) {
    evento.preventDefault();
    setEnviando(true);
    setErro(null);
    try {
      aoEntrar(await api.post('/auth/login', { email, senha }));
    } catch (e) {
      setErro(e.message);
    } finally {
      setEnviando(false);
    }
  }

  return (
    <main className="entrada">
      <h1>VGRFinance</h1>
      <p className="secundario">Planeje primeiro. Entenda depois.</p>
      <form onSubmit={entrar} className="cartao">
        <div>
          <label htmlFor="email">E-mail</label>
          <input id="email" type="email" autoComplete="username" required
                 value={email} onChange={(e) => setEmail(e.target.value)} />
        </div>
        <div>
          <label htmlFor="senha">Senha</label>
          <input id="senha" type="password" autoComplete="current-password" required
                 value={senha} onChange={(e) => setSenha(e.target.value)} />
        </div>
        <button className="primario" disabled={enviando}>{enviando ? 'Entrando…' : 'Entrar'}</button>
        <Mensagem erro={erro} />
      </form>
    </main>
  );
}
