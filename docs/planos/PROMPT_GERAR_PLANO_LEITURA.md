# Prompt para gerar plano de leitura (texto importável)

Use este texto como **system prompt** ou **instrução fixa** ao pedir a uma IA que converta um PDF ou lista de leituras diárias em texto pronto para importação na API Daily Bread.

---

## Objetivo

Gerar um arquivo de texto em que **cada linha** represente **um dia** do ano, no formato aceito pelo parser da API (`ReadingPlanPdfParser`):

- Padrão: `NÚMERO_DO_DIA NOME_DO_LIVRO CAPÍTULO_INICIAL-CAPÍTULO_FINAL`
- O número do dia vem **no início** da linha (1 a 365 ou 366).
- O nome do livro pode ter **várias palavras** (ex.: `I Samuel`, `II Reis`, `Cantares de Salomão`).
- O intervalo de capítulos usa **hífen**: `1-3`, `119-119`, `1-150`.
- **Sem** colunas extras tipo "Lido", datas ou referências de versículo — só dia, livro e capítulos.
- **Encoding:** UTF-8. Preserve acentuação em português nos nomes dos livros (Gênesis, Êxodo, Jeremias, etc.).

## Vários livros no mesmo dia (particularidade do sistema)

Em **qualquer** dia do plano (1…365 ou 1…366), se houver **duas ou três leituras em livros distintos** no mesmo dia civil, a API precisa de um formato explícito para montar todos os trechos na “Bíblia de hoje”.

**Formato obrigatório nesses casos:** **ponto e vírgula** (`;`) ou **sinal de mais** (`+`) entre trechos no mesmo dia. O **número do dia** aparece **apenas no primeiro** trecho.

- Com capítulos: `LIVRO cap_inicio-cap_fim` (ou um único capítulo: `LIVRO 5` significa cap. 5–5).
- **Só o nome do livro** (sem números no fim): na importação a API trata como **livro inteiro** (1 até o último capítulo na NVI).

Exemplo genérico (o `N` é o dia que for no plano):

`N LivroA 1-3; LivroB`

Três leituras curtas (só nomes = cada livro completo):

`N II João; III João; Judas`

Regras:

- Cada trecho depois do `;` deve ser `NOME_DO_LIVRO CAP_INICIO-CAP_FIM` (mesmo padrão de hífen que no trecho inicial).
- Pode haver **dois ou mais** trechos separados por `;` (o sistema interpreta todos).
- Nomes de livro devem ser **reconhecíveis** pela versão alvo (ex. NVI: **Filemom**, **Judas**, **2 João** ou equivalente **II João** conforme aliases do projeto).

**Outro padrão (vários dias na mesma linha de texto):** quando o PDF coloca **dias diferentes** na mesma linha (colunas), repita o número do dia em cada bloco:

`N1 LivroA a-b N2 LivroB c-d`

Exemplo:

`1 Gênesis 1-3  32 Levítico 4-6  60 Números 1-3`

(Espaço duplo entre blocos é aceito.)

## Outros formatos (um livro ou faixa composta)

1. **PDF com duas colunas por página:** ao extrair texto, linhas podem **colar** números e nomes. Separar cada `DIA LIVRO CAP-CAP` corretamente e **ordenar por dia**.
2. **Um salmo como leitura única:** se o plano pedir só um capítulo de Salmos, use uma linha `N Salmos 119-119` (ajuste `N` e o capítulo ao plano).
3. **Dois livros curtos tratados como uma faixa contínua no plano:** quando o material original usa um nome composto (ex. Obadias e Jonas numa única sequência de capítulos), pode ser uma linha `N Obadias e Jonas 1-4` — **desde que** isso corresponda a um único intervalo no modelo do plano. Se forem leituras **independentes** por livro, prefira o formato com `;`.

## Saída esperada

- Arquivo `.txt` com **365 linhas** (ou 366 em ano bisexto, se o plano usar), uma linha por dia, ordenadas por dia.
- Sem cabeçalhos repetidos no meio do arquivo (remover linhas como "Dia Leitura" que apareçam após quebra de página).
- Validar: todo número de 1 a N aparece **exatamente uma vez** como dia.

## Exemplo de trecho (formato correto)

Os números de dia abaixo são **ilustrativos**; o importante é o **padrão** de cada linha.

```
1 Gênesis 1-3
2 Gênesis 4-6
3 Gênesis 7-9
100 Salmos 119-119
200 Obadias e Jonas 1-4
240 Tito 1-3; Filemom
300 II João; III João; Judas
365 Apocalipse 21-22
```

## Referência no repositório

- **Script genérico** (qualquer plano): `docs/planos/normalize_reading_plan_extract.py`
  - Ex.: `python normalize_reading_plan_extract.py meu-extraido.txt -o meu-importavel.txt --expect-days 365`
  - Preset em `docs/planos/presets/`: `--preset d12-2026` (equivale a `--overrides presets/d12-2026-overrides.json`).
  - Outro PDF: crie `presets/meu-plano-overrides.json` e use `--preset meu-plano`, ou passe `--overrides /caminho/qualquer.json` (mapa `"42": "42 Gênesis 1-1"` etc.).
- **Atalho só D12 2026:** `docs/planos/normalize_d12_extract.py` (chama o genérico com os ficheiros D12 e o preset).
- Exemplo de plano completo: `docs/planos/plano-biblico-d12-2026-importavel.txt`

Ao gerar um plano novo, use as regras acima; o arquivo de exemplo serve como referência de estilo e de edge cases de um PDF concreto.
