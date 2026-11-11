// Só formatação para exibir. Nenhuma conta de dinheiro acontece no navegador:
// os valores chegam prontos da API e saem como texto decimal.

const REAL = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const PERCENTUAL = new Intl.NumberFormat('pt-BR', { style: 'percent', maximumFractionDigits: 1 });
const MESES = ['janeiro', 'fevereiro', 'março', 'abril', 'maio', 'junho', 'julho',
  'agosto', 'setembro', 'outubro', 'novembro', 'dezembro'];

/** R$ 1.234,56; negativo com o sinal de menos verdadeiro (−). */
export function moeda(valor) {
  return REAL.format(Number(valor)).replace('-', '−');
}

/** Sem sinal: um desvio de −150 vira "R$ 150,00" (o texto ao redor diz se sobra ou passa). */
export function moedaSemSinal(valor) {
  return REAL.format(Math.abs(Number(valor)));
}

/** Sempre com sinal: +R$ 112,00 ou −R$ 40,00. */
export function moedaComSinal(valor, positivo) {
  return `${positivo ? '+' : '−'}${moedaSemSinal(valor)}`;
}

/** Fração da API (0.8 ou 1.3733) vira "80%" ou "137,3%". */
export function percentual(fracao) {
  return PERCENTUAL.format(Number(fracao));
}

/** "2026-11-05" vira "05/11/2026" sem passar por Date (evita o dia anterior por fuso). */
export function data(texto) {
  const [ano, mes, dia] = texto.split('-');
  return `${dia}/${mes}/${ano}`;
}

/** "2026-11" vira "novembro de 2026". */
export function mesPorExtenso(texto) {
  const [ano, mes] = texto.split('-');
  return `${MESES[Number(mes) - 1]} de ${ano}`;
}

/** Data de hoje no formato da API ("2026-11-05"), pela data do computador da pessoa. */
export function hoje() {
  const agora = new Date();
  const mes = String(agora.getMonth() + 1).padStart(2, '0');
  const dia = String(agora.getDate()).padStart(2, '0');
  return `${agora.getFullYear()}-${mes}-${dia}`;
}

/** Mês atual no formato da API ("2026-11"). */
export function mesAtual() {
  return hoje().slice(0, 7);
}

/** O que a pessoa digitou ("1.234,56") vira o texto decimal da API ("1234.56"). */
export function paraDecimal(digitado) {
  return digitado.trim().replace(/\./g, '').replace(',', '.');
}

/** O valor da API (750 ou 1234.56) vira o formato de digitação ("750,00", "1234,56"). */
export function paraDigitacao(decimal) {
  return Number(decimal).toFixed(2).replace('.', ',');
}
