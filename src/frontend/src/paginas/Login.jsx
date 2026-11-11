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
      <section className="entrada-marca">
        <span className="marca">VGR<em>Finance</em></span>
        <h1>Planeje primeiro. Entenda depois.</h1>
        <ul>
          <li><strong>Antes do mês:</strong> saiba quanto destinar a cada categoria.</li>
          <li><strong>Durante:</strong> registre em segundos o que entra e o que sai.</li>
          <li><strong>Depois:</strong> entenda o que os seus números dizem.</li>
        </ul>
      </section>
      <form onSubmit={entrar} className="entrada-form">
        <h2>Entrar</h2>
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
