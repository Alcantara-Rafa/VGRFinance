// Única porta do front-end para a API. Cuida do cookie CSRF e traduz erros.

export class ErroApi extends Error {
  constructor(status, mensagem) {
    super(mensagem);
    this.status = status;
  }
}

function lerCookie(nome) {
  const par = document.cookie.split('; ').find((c) => c.startsWith(`${nome}=`));
  return par ? decodeURIComponent(par.slice(nome.length + 1)) : null;
}

// Toda escrita leva o token do cookie XSRF-TOKEN no cabeçalho X-XSRF-TOKEN.
async function tokenCsrf() {
  if (!lerCookie('XSRF-TOKEN')) {
    await fetch('/api/auth/csrf');
  }
  return lerCookie('XSRF-TOKEN');
}

async function chamar(metodo, caminho, corpo) {
  const cabecalhos = {};
  if (metodo !== 'GET') {
    cabecalhos['X-XSRF-TOKEN'] = await tokenCsrf();
  }
  if (corpo !== undefined) {
    cabecalhos['Content-Type'] = 'application/json';
  }
  const resposta = await fetch(`/api${caminho}`, {
    method: metodo,
    headers: cabecalhos,
    body: corpo === undefined ? undefined : JSON.stringify(corpo),
  });
  const dados = await resposta.json().catch(() => null);
  if (!resposta.ok) {
    throw new ErroApi(resposta.status, dados?.detail ?? 'Não foi possível concluir. Tente de novo.');
  }
  return dados;
}

export const api = {
  get: (caminho) => chamar('GET', caminho),
  post: (caminho, corpo) => chamar('POST', caminho, corpo),
  put: (caminho, corpo) => chamar('PUT', caminho, corpo),
  excluir: (caminho) => chamar('DELETE', caminho),
};
