A partir de agora, gere TODOS os commits em português brasileiro (pt-BR), com descrição objetiva apenas do que foi feito.

Regras obrigatórias para cada commit:
1) Mensagem em pt-BR.
2) Versionamento em todos os commits no padrão SemVer.
3) Corpo contendo somente as alterações realizadas.

Formato obrigatório:
<versao> <tipo>(<escopo>): <resumo curto em pt-BR>

Corpo do commit (obrigatório):
- Alterações:
    - <item 1 objetivo do que foi feito>
    - <item 2 objetivo do que foi feito>
    - <item 3 objetivo do que foi feito>

Padrão de versão:
- MAJOR (X.0.0): mudanças incompatíveis
- MINOR (0.X.0): novas funcionalidades compatíveis
- PATCH (0.0.X): correções e ajustes compatíveis

Tipos permitidos:
- feat, fix, refactor, docs, test, chore, perf, style

Exemplo:
v1.2.1 fix(auth): corrigir validação de login
- Alterações:
    - Ajustada validação de e-mail e senha no formulário de login
    - Corrigido redirecionamento após autenticação
    - Atualizados testes do fluxo de login