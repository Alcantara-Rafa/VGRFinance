/** Abertura de cada página: saudação, título e o que a página faz. */
export default function Hero({ saudacao, titulo, descricao }) {
  return (
    <header className="hero">
      <p className="hero-saudacao">{saudacao}</p>
      <h1>{titulo}</h1>
      <p className="hero-descricao">{descricao}</p>
    </header>
  );
}
