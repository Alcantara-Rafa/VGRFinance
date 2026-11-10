/** Mensagem de erro da API, anunciada para leitores de tela. */
export default function Mensagem({ erro }) {
  if (!erro) {
    return null;
  }
  return <p className="mensagem erro" role="alert">{erro}</p>;
}
