# Prompt para o front-end — mudanças da API (leitura, Bíblia, favoritos)

Use este documento como especificação para implementar telas e integração. A base da API continua `http://localhost:9090` (ou o host de produção). Rotas públicas da Bíblia permanecem sem autenticação; o restante exige JWT como antes.

---

## 1. Progresso de leitura — fluxos de “sem falhas” vs. “com falhas”

### Estatísticas (`GET /api/v1/reading-progress/statistics`)

A resposta JSON ganhou um campo **booleano**:

| Campo | Tipo | Significado |
|--------|------|-------------|
| `hasMissedDaysInPeriod` | `boolean` | `true` se existir pelo menos um dia **agendado** no período (entre `periodFrom` e `periodTo`, respeitando o início do plano) em que **não** houve leitura registrada. Equivale a `daysMissedInPeriod > 0`, mas está explícito para o UI. |

**Sugestão de UX**

- Se `hasMissedDaysInPeriod === false` no período que o usuário está configurando (ex.: ano ou intervalo escolhido), **não** mostrar opções para “adicionar dias sem ler” ou fluxos pesados de recuperação — apenas seleção de período simples, se fizer sentido no produto.
- Se `hasMissedDaysInPeriod === true`, mostrar o fluxo estendido: vários intervalos de datas, botão “+” para novo intervalo, etc.

**Compatibilidade:** clientes antigos que ignoram campos desconhecidos continuam válidos; quem deserializa o record/DTO precisa incluir o novo campo.

---

## 2. Desmarcar um dia do plano (não li / corrigir marcação)

**Novo endpoint (autenticado)**

- `DELETE /api/v1/reading-progress/days/{dayNumber}/read`  
- `dayNumber`: número do dia **dentro do plano** (1 … N), o mesmo usado em `POST /days/read`.

Remove o registro de conclusão desse dia. Se já não estava marcado, a operação é idempotente (nada a apagar).

**Uso no produto:** permitir que o usuário deixe um dia **sem marcar** após uma recuperação parcial, ou corrigir erros.

### 2.1. Várias leituras no mesmo dia — conclusão **por trecho** (obrigatório no UI)

**Problema resolvido:** antes, marcar “Gênesis 1–3” podia contar o dia inteiro; agora o **dia do plano** (calendário, contador “X de N dias”, streak) só avança quando **todos** os trechos daquele `dayNumber` estão marcados.

**Marcar leitura**

- `POST /api/v1/reading-progress/days/read`  
- Body JSON (campos novos **opcionais**):

| Campo | Tipo | Obrigatório | Significado |
|--------|------|-------------|-------------|
| `dayNumber` | `number` | sim | Dia dentro do plano (1…N), como hoje. |
| `readDate` | `string` (ISO date) | não | Data civil da leitura; omissão = hoje. |
| `readingPlanDayId` | `number` | não | ID do trecho (`planDayId` na resposta do dashboard / today bible). **Se enviado, marca só esse trecho.** |
| `segmentIndex` | `number` | não | Índice do bloco quando há **vários** blocos com o **mesmo** `planDayId` (texto legado com `;` numa única linha). Omissão = `0`. |

**Regras para o front:**

1. **Cada cartão / bloco** de “Leitura de hoje” deve chamar o POST com `dayNumber` **e** `readingPlanDayId` (e `segmentIndex` se a API devolver `segmentIndex !== 0` ou se o modelo tiver semicolon-split).
2. Os valores vêm das respostas:
   - `GET /api/v1/reading-progress/dashboard` → `today.blocks[]` com `planDayId`, `segmentIndex`, `completed` **por bloco**.
   - `GET /api/v1/reading-progress/today/bible` → `blocks[]` com os mesmos campos; `dayCompleted` no topo = **dia totalmente fechado** (todos os trechos).
3. **`completed` por bloco:** refletir no UI (check, cor, etc.) **independentemente** dos outros blocos do mesmo dia.
4. **`dayCompleted` / contador de dias / calendário:** só devem refletir “dia feito” quando a API indicar dia completo (ex.: `dayCompleted === true` ou ausência de blocos pendentes — o backend já só grava o dia no calendário quando todos os trechos estão ok).

**Marcar o dia inteiro de uma vez** (recuperação, botão global, clientes antigos)

- Enviar **apenas** `dayNumber` e opcionalmente `readDate`, **sem** `readingPlanDayId`.  
- Comportamento: marca **todos** os trechos daquele dia e fecha o dia no calendário.

**Desmarcar um trecho só**

- `DELETE /api/v1/reading-progress/plan-days/{planDayId}/read?segmentIndex=0`  
- `segmentIndex` opcional na query; padrão `0`.  
- Remove só essa conclusão; se não restar trecho marcado naquele dia, o dia **sai** do calendário / contador.

**Desmarcar o dia inteiro** (inalterado)

- `DELETE /api/v1/reading-progress/days/{dayNumber}/read` — remove **todos** os trechos daquele dia e o registro de dia completo.

**Compatibilidade / migração:** utilizadores que já tinham dias marcados na versão antiga recebem, na migração da BD, um registo de trecho por cada linha física do plano naquele dia; dias com uma única linha e texto com `;` podem precisar que o utilizador marque trechos extra até o produto estabilizar (caso raro).

---

## 3. Recuperação (catch-up) — um intervalo ou vários

### Intervalo único (já existia, comportamento ajustado)

- `POST /api/v1/reading-progress/catch-up/date-range`  
- Body: `{ "fromInclusive": "2026-01-01", "toInclusive": "2026-01-31" }`  

**Mudança:** cada intervalo passa a respeitar o **limite de 366 dias** (inclusive), igual ao calendário. Intervalos maiores retornam erro 400 com a mensagem já usada em outros endpoints de data.

### Vários intervalos (novo)

- `POST /api/v1/reading-progress/catch-up/date-ranges`  
- Body:

```json
{
  "ranges": [
    { "fromInclusive": "2026-01-01", "toInclusive": "2026-01-05" },
    { "fromInclusive": "2026-02-10", "toInclusive": "2026-02-12" }
  ]
}
```

Cada item segue as mesmas regras do intervalo único (datas válidas, até 366 dias por intervalo). Os intervalos são aplicados **em sequência**; dias repetidos entre intervalos são apenas atualizados de novo (idempotente).

**Sugestão de UX:** o botão “+” adiciona mais um par início/fim no formulário e, no envio, monta o array `ranges`.

---

## 4. Regra de negócio: plano por calendário (importante para copy/UX)

A API continua alinhando **data civil** → **dia N do plano** (`N = dias desde o início do plano + 1`).  

Se o usuário **não lê** em um dia e **não marca**, no dia seguinte a API já expõe o **próximo** dia do plano para aquela data — ou seja, **não** há fila automática que “empurra” a leitura atrasada para o dia seguinte no backend. Qualquer narrativa do tipo “o que faltou será lido amanhã” precisa ser **decisão de produto no app** (ex.: aviso educativo), não é modelada como fila nesta API.

---

## 5. Bíblia — grade de capítulos (quadrados)

**`GET /api/v1/bible/{version}/books`**

Cada item da lista agora inclui:

| Campo | Tipo | Significado |
|--------|------|-------------|
| `chapterCount` | `number` | Quantidade de capítulos daquele livro **nessa versão** (ex.: Salmos 150). |

**Sugestão de UX:** renderizar uma grade 1 … `chapterCount` para pular direto ao capítulo, sem depender só do botão “próximo”.

---

## 6. Favoritos de versículos (leitura diária + calendário)

**Autenticado.** Migração de banco cria a tabela `user_verse_favorites`.

### Criar favorito

- `POST /api/v1/verse-favorites`  
- Body:

```json
{
  "versionId": "nvi",
  "bookNumber": 1,
  "chapterNumber": 1,
  "verseNumber": 1,
  "readingDate": "2026-04-19"
}
```

- `readingDate`: data do **calendário** em que o usuário associou o versículo (ex.: dia aberto no plano / no calendário da app).
- Resposta **201** com o objeto criado (inclui `id`, texto do versículo, nomes do livro, etc.).

**Conflito (409):** mesmo usuário, mesma referência (`versionId` + livro + capítulo + versículo) + mesma `readingDate`.

### Listar favoritos de um dia

- `GET /api/v1/verse-favorites?readingDate=2026-04-19`

Útil ao tocar em um dia no **calendário**: mostrar os versículos favoritos daquela data.

### Remover

- `DELETE /api/v1/verse-favorites/{id}`  

O `id` vem da criação ou da listagem.

**Sugestão de UX**

- Ao sair da tela de leitura e voltar, **recarregar** a lista de favoritos (ou o estado do ícone) com `GET` por data ou invalidar cache — a API persiste o favorito; o ícone deve refletir o estado do servidor após nova carga.

---

## 7. Resumo rápido para o time de front

| Área | O que fazer |
|------|-------------|
| Estatísticas | Ler `hasMissedDaysInPeriod` para bifurcar fluxo simples vs. recuperação com múltiplos intervalos. |
| Marcação | Usar `DELETE .../days/{dayNumber}/read` para desmarcar. |
| Catch-up | Vários blocos de datas → `POST .../catch-up/date-ranges`. |
| Navegação Bíblia | Usar `chapterCount` em cada livro para UI em grade. |
| Favoritos | `POST` / `GET ?readingDate=` / `DELETE` em `/api/v1/verse-favorites`. |

---

## 8. Fora do escopo desta API

- Bug intermitente do botão “world” / internacionalização após login: **front apenas** (não há mudança de API relacionada).
