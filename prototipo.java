// Permite armazenar conjuntos de constantes de um enum de forma compacta e rápida; usado em ModuloAutoridades (EnumSet<Autoridade>) para permissões por cargo/funcionário
import java.util.EnumSet;
// Mapa que mantém a ordem de inserção; usado em ModuloAutoridades (autoridadesPorId) e em exibirRelatorioVolumeComprasClientes (totaisPorCliente e pedidosPorCliente)
import java.util.LinkedHashMap;
// Representa um conjunto sem elementos repetidos; usado na constante UFS_BRASIL (validação de UF) e em calcularAliquotaIcmsPorOperacao (UFs do Sul/Sudeste)
import java.util.Set;
// Listas dinâmicas usadas nas tabelas paralelas (funcionários, fornecedores, produtos, clientes, transações), paginação e filtros
import java.util.ArrayList;
// Usado no main para inicializar indicadores em List<Double> de 1 elemento (ex.: Arrays.asList(200000.00)) permitindo mutação por referência
import java.util.Arrays;
// Interface de lista usada em praticamente todos os métodos e módulos para armazenar colunas paralelas, registros filtrados e históricos
import java.util.List;
// Interface de mapa usada na assinatura de métodos que consolidam agrupamentos (como totais e pedidos por cliente)
import java.util.Map;
// Usado no main e nos métodos de entrada/validação (lerLinhaSegura, validarELer*) para ler comandos e dados digitados no console
import java.util.Scanner;
// Leitura linha a linha de arquivos .TXT; usado em ModuloLogDadosTxt e ModuloPersistenciaTxt para ler pré-cadastros e restaurar backup
import java.io.BufferedReader;
// Escrita de texto em arquivos .TXT; usado para gravar logs, pré-cadastros padrão, backups e relatórios exportados
import java.io.BufferedWriter;
// Representa caminhos/arquivos no disco; usado para verificar existência de arquivos (.exists()) antes de ler, sobrescrever ou restaurar
import java.io.File;
// Fluxo de leitura de arquivo conectado ao BufferedReader ao carregar dados e logs dos arquivos .TXT
import java.io.FileReader;
// Fluxo de escrita de arquivo conectado ao BufferedWriter ao salvar logs, backups e relatórios em .TXT
import java.io.FileWriter;
// Tratamento de exceções de entrada/saída (try/catch) nas operações de leitura e gravação de arquivos .TXT
import java.io.IOException;
// Captura a data e hora atuais do sistema em ModuloAuditoria, ModuloLogDadosTxt, ModuloMovimentacaoEstoque e cabeçalhos de exportação
import java.time.LocalDateTime;
// Formata LocalDateTime no padrão dd/MM/yyyy HH:mm:ss (FORMATADOR_DATA_HORA) para carimbo de tempo em logs, auditoria e arquivos .TXT
import java.time.format.DateTimeFormatter;
// Formata valores numéricos como moeda em formatarValorMonetarioReais usando FORMATADOR_MOEDA_BR
import java.text.NumberFormat;
// Define a localidade "pt", "BR" para formatar moeda em reais (R$) no NumberFormat
import java.util.Locale;
// Gera números aleatórios de forma segura em gerarSenhaTemporaria() para senhas iniciais, de readmissão, reset ou bloqueio
import java.util.concurrent.ThreadLocalRandom;

public class prototipo {

    // =========================================================================
    // MÓDULO 0: CONSTANTES GLOBAIS E UTILITÁRIOS DE FORMATAÇÃO / VALIDAÇÃO
    // Independente de estado de sessão ou instância; fornece constantes de
    // arquivos, formatação monetária/data-hora, cores ANSI e regras de entrada.
    // =========================================================================

    // Constantes de nomes de arquivos e formatação temporal/monetária do sistema
    private static final String ARQUIVO_PRE_CADASTROS = "dados_pre_cadastros.txt";
    private static final String ARQUIVO_BACKUP_OFICIAL = "backup_sistema_editavel.txt";
    private static final String ARQUIVO_LOG_DADOS = "log_entradas_saidas_dados.txt";
    private static final String ARQUIVO_LOG_FILTRADO_EXPORTADO = "log_entradas_saidas_filtrado.txt";
    private static final DateTimeFormatter FORMATADOR_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    // Formata valores monetários no padrão brasileiro usando Locale.of (evita aviso de construtor depreciado no Java 19+)
    private static final NumberFormat FORMATADOR_MOEDA_BR = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

    // Conjunto imutável com as 27 unidades federativas do Brasil para validação rápida de UF
    private static final Set<String> UFS_BRASIL = Set.of(
            "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG",
            "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO"
    );

    // Utilitário de formatação ANSI para destacar títulos, avisos, erros, sucessos e cores no terminal
    static class CoresAnsi {
        static final String RESET = "\u001B[0m";
        static final String CIANO = "\u001B[36m";
        static final String VERDE = "\u001B[32m";
        static final String AMARELO = "\u001B[33m";
        static final String VERMELHO = "\u001B[31m";
        static final String NEGRITO = "\u001B[1m";

        // Aplica cor genérica à mensagem informada
        static String colorir(String cor, String texto) {
            return cor + texto + RESET;
        }

        // Formata cabeçalhos e molduras do sistema
        static String titulo(String texto) {
            return NEGRITO + CIANO + texto + RESET;
        }

        // Formata mensagens de confirmação e estados positivos
        static String sucesso(String texto) {
            return VERDE + texto + RESET;
        }

        // Formata alertas, pendências e avisos intermediários
        static String aviso(String texto) {
            return AMARELO + texto + RESET;
        }

        // Formata mensagens de erro e validações inválidas
        static String erro(String texto) {
            return VERMELHO + texto + RESET;
        }
    }

    // Enumeração ampliada com permissões granulares para RH, Financeiro, Gerente e áreas operacionais
    enum Autoridade {
        RH_VER_FOLHA_E_STATUS("Visualizar status de pagamento da folha e histórico funcional"),
        RH_VER_FUNCIONARIOS("Visualizar lista de funcionários"),
        RH_GERIR_CADASTROS("Cadastrar, editar e alterar status de funcionários"),
        RH_ALTERAR_CARGO_SALARIO("Promover, rebaixar ou ajustar cargo e salário de funcionários"),
        RH_INATIVAR_E_READMITIR("Inativar/desligar ou readmitir registros de funcionários"),
        RH_RESETAR_SENHA("Redefinir senha de funcionário para temporária"),
        RH_GERIR_AUTORIDADES("Conceder ou revogar autoridades de funcionários"),
        FORN_VER_LISTA("Visualizar registros de fornecedores"),
        FORN_GERIR_OPERACOES("Registrar compras, editar ou remover fornecedores"),
        FORN_QUITAR_FATURAS("Registrar quitação de faturas de fornecedores"),
        EST_VER_CATALOGO("Visualizar catálogo e saldos de estoque"),
        EST_GERIR_CATALOGO("Cadastrar itens, editar preços, ajustar saldos e ver histórico de estoque"),
        EST_AJUSTAR_SALDO_E_PRECO("Ajustar saldo físico e preço de venda de itens no catálogo"),
        FIN_VER_VALORES_SENSIVEIS("Visualizar salários, caixa, lançamentos e demonstrativos financeiros"),
        FIN_LANCAR_ENTRADAS_SAIDAS("Registrar entradas e saídas financeiras no caixa"),
        FIN_PAGAR_E_ESTORNAR("Registrar pagamentos/quitações e estornar lançamentos"),
        FIN_ESTORNAR_LANCAMENTOS("Estornar lançamentos financeiros registrados no sistema"),
        FIN_VER_CONSOLIDADO_E_IMPOSTOS("Visualizar demonstrativo consolidado, margens e provisões de ICMS"),
        FIN_EXPORTAR_RELATORIOS("Exportar relatórios financeiros e de compras por cliente em .TXT"),
        CLI_GERIR_CADASTROS("Consultar e gerenciar cadastros de clientes"),
        LOG_VER_E_EXPORTAR("Consultar, filtrar e exportar logs de entradas/saídas"),
        SIS_AUDITORIA_E_BACKUP("Acessar auditoria, salvar backup e restaurar arquivos .TXT"),
        GER_SUPERVISAO_GERAL("Acesso de supervisão gerencial e visão global de indicadores"),
        GER_APROVAR_OPERACOES_CRITICAS("Autorizar e confirmar operações críticas do sistema");

        private final String descricao;

        Autoridade(String descricao) {
            this.descricao = descricao;
        }

        String getDescricao() {
            return descricao;
        }
    }

    // =========================================================================
    // MÓDULO 1: AUTORIDADES (RBAC)
    // Responsável exclusivamente por associar e consultar permissões por ID,
    // cargo e status ativo, sem depender de interface ou arquivos externos.
    // =========================================================================
    static class ModuloAutoridades {
        private final Map<String, EnumSet<Autoridade>> autoridadesPorId = new LinkedHashMap<>();

        // Define o conjunto inicial de autoridades de acordo com o cargo
        // GERENTE recebe todas as permissões do enum; FINANCEIRO e OPERADOR recebem conjuntos delimitados
        static EnumSet<Autoridade> autoridadesPadraoPorCargo(String cargo) {
            // Se o cargo for nulo, não concede nenhuma autoridade
            if (cargo == null) return EnumSet.noneOf(Autoridade.class);

            // Seleciona as permissões padrão conforme o perfil/cargo
            switch (cargo.trim().toUpperCase()) {
                case "GERENTE":
                    // Gerente recebe todas as autoridades cadastradas no enum
                    return EnumSet.allOf(Autoridade.class);
                case "FINANCEIRO":
                    // Financeiro concentra movimentações, pagamentos, estornos, relatórios e consulta de RH/estoque
                    return EnumSet.of(
                            Autoridade.RH_VER_FUNCIONARIOS,
                            Autoridade.RH_VER_FOLHA_E_STATUS,
                            Autoridade.RH_GERIR_CADASTROS,
                            Autoridade.FORN_VER_LISTA,
                            Autoridade.FORN_GERIR_OPERACOES,
                            Autoridade.FORN_QUITAR_FATURAS,
                            Autoridade.EST_VER_CATALOGO,
                            Autoridade.EST_GERIR_CATALOGO,
                            Autoridade.EST_AJUSTAR_SALDO_E_PRECO,
                            Autoridade.FIN_VER_VALORES_SENSIVEIS,
                            Autoridade.FIN_LANCAR_ENTRADAS_SAIDAS,
                            Autoridade.FIN_PAGAR_E_ESTORNAR,
                            Autoridade.FIN_ESTORNAR_LANCAMENTOS,
                            Autoridade.FIN_VER_CONSOLIDADO_E_IMPOSTOS,
                            Autoridade.FIN_EXPORTAR_RELATORIOS,
                            Autoridade.CLI_GERIR_CADASTROS,
                            Autoridade.LOG_VER_E_EXPORTAR
                    );
                case "OPERADOR":
                    // Operador recebe permissões básicas de visualização de estoque/fornecedores e consulta de logs
                    return EnumSet.of(
                            Autoridade.EST_VER_CATALOGO,
                            Autoridade.FORN_VER_LISTA,
                            Autoridade.LOG_VER_E_EXPORTAR
                    );
                default:
                    // Cargos desconhecidos ou sem função não recebem permissões
                    return EnumSet.noneOf(Autoridade.class);
            }
        }

        // Sincroniza autoridades de funcionários ativos com o cargo vigente quando não houver registro prévio
        void sincronizarComTabelaFuncionarios(List<String> ids, List<String> cargos, List<String> status) {
            if (ids == null || cargos == null || status == null) return;
            for (int i = 0; i < ids.size(); i++) {
                String id = ids.get(i);
                boolean ativo = i < status.size() && "ATIVO".equalsIgnoreCase(status.get(i));
                String cargo = i < cargos.size() ? cargos.get(i) : "SEM_CARGO";
                if (!ativo) {
                    autoridadesPorId.put(id, EnumSet.noneOf(Autoridade.class));
                } else if (!autoridadesPorId.containsKey(id)) {
                    autoridadesPorId.put(id, autoridadesPadraoPorCargo(cargo));
                }
            }
        }

        // Redefine o conjunto de autoridades de um ID de acordo com o cargo e o status ativo
        void redefinirPorCargo(String id, String cargo, boolean ativo) {
            if (id == null || id.isBlank()) return;
            if (!ativo) {
                autoridadesPorId.put(id, EnumSet.noneOf(Autoridade.class));
            } else {
                autoridadesPorId.put(id, autoridadesPadraoPorCargo(cargo));
            }
        }

        // Alterna ou define individualmente uma autoridade para o ID informado
        void definirAutoridade(String id, Autoridade autoridade, boolean conceder) {
            if (id == null || id.isBlank() || autoridade == null) return;
            EnumSet<Autoridade> set = autoridadesPorId.computeIfAbsent(id, k -> EnumSet.noneOf(Autoridade.class));
            if (conceder) {
                set.add(autoridade);
            } else {
                set.remove(autoridade);
            }
        }

        // Retorna cópia defensiva das autoridades do funcionário caso esteja ativo
        EnumSet<Autoridade> obterAutoridades(String id, boolean ativo) {
            if (!ativo || id == null) return EnumSet.noneOf(Autoridade.class);
            EnumSet<Autoridade> set = autoridadesPorId.get(id);
            return (set == null) ? EnumSet.noneOf(Autoridade.class) : EnumSet.copyOf(set);
        }

        // Verifica se o funcionário ativo possui a autoridade consultada
        boolean possuiAutoridade(String id, boolean ativo, Autoridade autoridade) {
            if (!ativo || id == null || autoridade == null) return false;
            EnumSet<Autoridade> set = autoridadesPorId.get(id);
            return set != null && set.contains(autoridade);
        }
    }

    // =========================================================================
    // MÓDULO 2: SESSÃO E VALIDAÇÃO DE ACESSO
    // Responsável por manter a identidade do usuário logado, validar status
    // ativo, conferir senha vigente, obter cargo vigente e checar permissões.
    // =========================================================================
    static class ModuloSessao {
        private final String idUsuario;
        private final List<String> tabelaId;
        private final List<String> tabelaNome;
        private final List<String> tabelaCargo;
        private final List<String> tabelaSenha;
        private final List<String> tabelaStatus;
        private final ModuloAutoridades moduloAutoridades;
        private boolean sessaoRevogada = false;

        ModuloSessao(String idUsuario, List<String> tabelaId, List<String> tabelaNome,
                     List<String> tabelaCargo, List<String> tabelaSenha, List<String> tabelaStatus,
                     ModuloAutoridades moduloAutoridades) {
            this.idUsuario = idUsuario;
            this.tabelaId = tabelaId;
            this.tabelaNome = tabelaNome;
            this.tabelaCargo = tabelaCargo;
            this.tabelaSenha = tabelaSenha;
            this.tabelaStatus = tabelaStatus;
            this.moduloAutoridades = moduloAutoridades;
        }

        // Retorna o ID do usuário autenticado na sessão
        String obterIdVigente() {
            return idUsuario;
        }

        // Localiza o índice atual do usuário na tabela de funcionários
        int indiceAtual() {
            return tabelaId.indexOf(idUsuario);
        }

        // Retorna o nome atualizado do usuário logado
        // Protege contra IndexOutOfBoundsException caso o índice esteja fora dos limites da lista paralela
        String obterNomeVigente() {
            int idx = indiceAtual();
            return (idx >= 0 && idx < tabelaNome.size()) ? tabelaNome.get(idx) : "DESCONHECIDO";
        }

        // Retorna o cargo vigente do usuário autenticado na tabela de funcionários
        String obterCargoVigente() {
            int idx = indiceAtual();
            return (idx >= 0 && idx < tabelaCargo.size()) ? tabelaCargo.get(idx) : "SEM_CARGO";
        }

        // Confere se a senha informada corresponde à senha vigente do usuário ativo na sessão
        boolean senhaAtualCorresponde(String senhaInformada) {
            if (!estaAtiva() || senhaInformada == null) return false;
            int idx = indiceAtual();
            return idx >= 0 && idx < tabelaSenha.size() && senhaInformada.equals(tabelaSenha.get(idx));
        }

        // Indica se a sessão ainda está ativa e válida
        boolean estaAtiva() {
            if (sessaoRevogada) return false;
            int idx = indiceAtual();
            return idx >= 0 && idx < tabelaStatus.size() && "ATIVO".equalsIgnoreCase(tabelaStatus.get(idx));
        }

        // Revalida o status da conta e revoga a sessão caso o usuário tenha sido inativado/removido
        boolean validarOuRevogarSessao() {
            if (!estaAtiva()) {
                if (!sessaoRevogada) {
                    sessaoRevogada = true;
                    System.out.println(CoresAnsi.erro("\n[SESSÃO ENCERRADA]: Sua conta não está mais ativa.\n"));
                }
                return false;
            }
            return true;
        }

        // Verifica se a sessão ativa possui a autoridade solicitada
        boolean temAutoridade(Autoridade autoridade) {
            if (!estaAtiva() || moduloAutoridades == null) return false;
            return moduloAutoridades.possuiAutoridade(idUsuario, true, autoridade);
        }

        // Exige a autoridade para prosseguir e exibe mensagem de bloqueio caso ausente
        boolean exigirAutoridade(Autoridade autoridade, String mensagemBloqueio) {
            if (!validarOuRevogarSessao()) return false;
            if (!temAutoridade(autoridade)) {
                System.out.println(CoresAnsi.erro(" " + mensagemBloqueio + "\n"));
                return false;
            }
            return true;
        }

        // Mascara valores sensíveis caso o usuário não tenha permissão de visualização financeira
        String exibirValorCondicional(Autoridade autoridade, double valor) {
            if (temAutoridade(autoridade)) {
                return formatarValorMonetarioReais(valor);
            }
            return "R$ ***.** (RESTRITO)";
        }
    }

    // =========================================================================
    // MÓDULO 3: AUDITORIA E LOGS DE DADOS / ESTOQUE
    // Independentes entre si e desacoplados da lógica de negócios: registram
    // auditoria interna, log de entradas/saídas em .TXT e histórico de estoque.
    // =========================================================================

    // Registra eventos de auditoria com carimbo de data/hora e consulta filtrada
    static class ModuloAuditoria {
        private final List<String> registros = new ArrayList<>();

        // Registra um evento com nome do operador e descrição da ação
        // Inclui carimbo de data e hora para facilitar rastreabilidade em auditorias e testes
        void registrar(String operador, String acao) {
            String timestamp = LocalDateTime.now().format(FORMATADOR_DATA_HORA);
            String autor = (operador == null || operador.isBlank()) ? "SISTEMA" : sanitizarCampoTxt(operador.trim());
            String desc = (acao == null) ? "" : sanitizarCampoTxt(acao.trim());
            registros.add("[" + timestamp + "] [AUDITORIA] Operador: " + autor + " | Ação: " + desc);
        }

        boolean estaVazio() {
            return registros.isEmpty();
        }

        List<String> buscarFiltrados(String termo) {
            if (termo == null || termo.isBlank()) return new ArrayList<>(registros);
            String busca = termo.trim().toUpperCase();
            List<String> filtrados = new ArrayList<>();
            for (String r : registros) {
                if (r.toUpperCase().contains(busca)) filtrados.add(r);
            }
            return filtrados;
        }
    }

    // Grava em arquivo .TXT o log de entradas e saídas de dados e movimentos
    static class ModuloLogDadosTxt {
        private final String caminhoArquivo;
        private final List<String> eventosEmMemoria = new ArrayList<>();

        ModuloLogDadosTxt(String caminhoArquivo) {
            this.caminhoArquivo = caminhoArquivo;
            carregarExistentes();
        }

        // Carrega linhas já existentes no arquivo de log ao iniciar
        private void carregarExistentes() {
            File arq = new File(caminhoArquivo);
            if (!arq.exists()) return;
            try (BufferedReader br = new BufferedReader(new FileReader(arq))) {
                String linha;
                while ((linha = br.readLine()) != null) {
                    if (!linha.isBlank()) eventosEmMemoria.add(linha);
                }
            } catch (IOException ignored) {}
        }

        // Acrescenta um evento de entrada/saída com timestamp na memória e no arquivo .TXT
        void registrarEvento(String direcao, String origemOuOperador, String detalhe) {
            String timestamp = LocalDateTime.now().format(FORMATADOR_DATA_HORA);
            String linha = String.format("[%s] [%s] %s -> %s",
                    timestamp,
                    sanitizarCampoTxt(direcao),
                    sanitizarCampoTxt(origemOuOperador),
                    sanitizarCampoTxt(detalhe));
            eventosEmMemoria.add(linha);
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(caminhoArquivo, true))) {
                bw.write(linha);
                bw.newLine();
            } catch (IOException ignored) {}
        }

        List<String> lerEventosFiltrados(String filtro) {
            if (filtro == null || filtro.isBlank()) return new ArrayList<>(eventosEmMemoria);
            String busca = filtro.trim().toUpperCase();
            List<String> resultado = new ArrayList<>();
            for (String ev : eventosEmMemoria) {
                if (ev.toUpperCase().contains(busca)) resultado.add(ev);
            }
            return resultado;
        }
    }

    // Registra histórico detalhado das movimentações físicas de estoque
    static class ModuloMovimentacaoEstoque {
        private final List<String> movimentos = new ArrayList<>();

        // Registra entrada, saída, cadastro ou ajuste de estoque
        void registrarMovimento(String tipoMovimento, String item, int variacaoQtd, int saldoApos, String referencia, String operador) {
            String timestamp = LocalDateTime.now().format(FORMATADOR_DATA_HORA);
            String sinal = variacaoQtd > 0 ? "+" + variacaoQtd : String.valueOf(variacaoQtd);
            String linha = String.format("[%s] [%s] Item: %s | Variação: %s un | Saldo: %d un | Ref: %s | Op: %s",
                    timestamp,
                    sanitizarCampoTxt(tipoMovimento),
                    sanitizarCampoTxt(item),
                    sinal,
                    saldoApos,
                    sanitizarCampoTxt(referencia),
                    sanitizarCampoTxt(operador));
            movimentos.add(linha);
        }

        List<String> buscarFiltrados(String filtro) {
            if (filtro == null || filtro.isBlank()) return new ArrayList<>(movimentos);
            String busca = filtro.trim().toUpperCase();
            List<String> resultado = new ArrayList<>();
            for (String m : movimentos) {
                if (m.toUpperCase().contains(busca)) resultado.add(m);
            }
            return resultado;
        }
    }

    // =========================================================================
    // MÓDULO 4: CAIXA E LANÇAMENTOS FINANCEIROS
    // Controla o saldo global, entradas/saídas financeiras e registro no log
    // de movimentação de dados sem depender de telas ou persistência em .TXT.
    // =========================================================================
    static class ModuloCaixa {
        private final List<Double> saldoGlobal;
        private final List<String> transacaoTipo;
        private final List<String> transacaoEnvolvidos;
        private final List<String> transacaoDescricao;
        private final List<Double> transacaoValor;
        private final ModuloLogDadosTxt logDados;

        ModuloCaixa(List<Double> saldoGlobal,
                    List<String> transacaoTipo,
                    List<String> transacaoEnvolvidos,
                    List<String> transacaoDescricao,
                    List<Double> transacaoValor,
                    ModuloLogDadosTxt logDados) {
            this.saldoGlobal = saldoGlobal;
            this.transacaoTipo = transacaoTipo;
            this.transacaoEnvolvidos = transacaoEnvolvidos;
            this.transacaoDescricao = transacaoDescricao;
            this.transacaoValor = transacaoValor;
            this.logDados = logDados;
        }

        // Retorna o saldo atual do caixa global
        double obterSaldo() {
            return (saldoGlobal == null || saldoGlobal.isEmpty()) ? 0.0 : saldoGlobal.get(0);
        }

        // Define diretamente um novo valor para o saldo global
        void definirSaldo(double novoSaldo) {
            if (saldoGlobal != null && !saldoGlobal.isEmpty() && Double.isFinite(novoSaldo)) {
                saldoGlobal.set(0, novoSaldo);
            }
        }

        // Registra movimentação de entrada/saída de dados no log externo
        void registrarMovimentoDados(String direcao, String origemOuOperador, String detalhe) {
            if (logDados != null) {
                logDados.registrarEvento(direcao, origemOuOperador, detalhe);
            }
        }

        // Credita um valor positivo no caixa e registra a transação/log
        boolean registrarEntrada(double valor, String tipo, String envolvidosStr, String descricao) {
            // Garante que a lista de saldo exista antes de atualizar o índice 0
            if (saldoGlobal == null || saldoGlobal.isEmpty()) {
                System.out.println(CoresAnsi.erro("Saldo global não inicializado.\n"));
                return false;
            }
            if (!Double.isFinite(valor) || valor <= 0.0) {
                System.out.println(CoresAnsi.erro("Valor de entrada inválido.\n"));
                return false;
            }
            // Soma o valor ao saldo atual
            saldoGlobal.set(0, obterSaldo() + valor);
            transacaoTipo.add(tipo);
            transacaoEnvolvidos.add(envolvidosStr);
            transacaoDescricao.add(descricao);
            transacaoValor.add(valor);
            registrarMovimentoDados("ENTRADA_CAIXA", envolvidosStr, tipo + " (" + formatarValorMonetarioReais(valor) + ") - " + descricao);
            return true;
        }

        // Debita um valor positivo do caixa após conferir saldo disponível
        boolean registrarSaida(double valor, String tipo, String envolvidosStr, String descricao) {
            if (saldoGlobal == null || saldoGlobal.isEmpty()) {
                System.out.println(CoresAnsi.erro("Saldo global não inicializado.\n"));
                return false;
            }
            if (!Double.isFinite(valor) || valor <= 0.0) {
                System.out.println(CoresAnsi.erro("Valor de saída inválido.\n"));
                return false;
            }
            if (obterSaldo() < valor) {
                System.out.println(CoresAnsi.erro("Saldo insuficiente em caixa (" + formatarValorMonetarioReais(obterSaldo()) + ") para operação de " + formatarValorMonetarioReais(valor) + ".\n"));
                return false;
            }
            saldoGlobal.set(0, obterSaldo() - valor);
            transacaoTipo.add(tipo);
            transacaoEnvolvidos.add(envolvidosStr);
            transacaoDescricao.add(descricao);
            transacaoValor.add(valor);
            registrarMovimentoDados("SAIDA_CAIXA", envolvidosStr, tipo + " (" + formatarValorMonetarioReais(valor) + ") - " + descricao);
            return true;
        }
    }

    // =========================================================================
    // MÓDULO 5: PERSISTÊNCIA EM ARQUIVO .TXT
    // Responsável por carregar pré-cadastros, gravar backups editáveis e
    // restaurar o backup oficial sem manter campos de snapshot não utilizados.
    // =========================================================================
    static class ModuloPersistenciaTxt {
        private final String arquivoPreCadastros;
        private final String arquivoBackupOficial;

        ModuloPersistenciaTxt(String arquivoPreCadastros, String arquivoBackupOficial) {
            this.arquivoPreCadastros = arquivoPreCadastros;
            this.arquivoBackupOficial = arquivoBackupOficial;
        }

        String obterNomeArquivoBackup() {
            return arquivoBackupOficial;
        }

        // Garante que exista um arquivo inicial de pré-cadastros caso ainda não esteja no disco
        private void criarArquivoPreCadastrosPadraoSeAusente() {
            File arq = new File(arquivoPreCadastros);
            if (arq.exists()) return;
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(arq))) {
                bw.write("# Arquivo de pré-cadastros iniciais do sistema\n");
                bw.write("[FUNCIONARIOS]\n");
                bw.write("001;MARIANA SILVA;12345678909;GERENTE;admin123;9500.00;false;ATIVO;NENHUM\n");
                bw.write("002;CARLOS SOUZA;98765432100;FINANCEIRO;fin123;6200.00;false;ATIVO;NENHUM\n");
                bw.write("003;LUCAS PEREIRA;11144477735;OPERADOR;op123;3800.00;false;ATIVO;NENHUM\n");
                bw.write("[FORNECEDORES]\n");
                bw.write("DISTRIBUIDORA PAULISTA LTDA;11222333000181;SP;CONCENTRADO CITRICO;4500.00;40.0;true\n");
                bw.write("[PRODUTOS]\n");
                bw.write("SUCO INTEGRAL LARANJA 1L;2009.19.00;18.50;120\n");
                bw.write("NECTAR MANGA 1L;2009.89.90;12.00;85\n");
                bw.write("[CLIENTES]\n");
                bw.write("52998224725;ANA BEATRIZ COSTA\n");
            } catch (IOException ignored) {}
        }

        // Carrega pré-cadastros de funcionários, fornecedores, produtos e clientes a partir do .TXT
        boolean conferirECarregarPreCadastrosExternos(
                List<String> tabelaFuncId, List<String> tabelaFuncNome, List<String> tabelaFuncCpf,
                List<String> tabelaFuncCargo, List<String> tabelaFuncSenha, List<Double> tabelaFuncSalario,
                List<Boolean> tabelaFuncPago, List<String> tabelaFuncStatus, List<String> tabelaFuncMotivoInativo,
                List<String> tabelaFornRazaoSocial, List<String> tabelaFornCnpj, List<String> tabelaFornUf,
                List<String> tabelaFornProdutoPrincipal, List<Double> tabelaFornValorFatura,
                List<Double> tabelaFornMargemLucroPct, List<Boolean> tabelaFornFaturaPaga,
                List<String> tabelaProdNome, List<String> tabelaProdNcm, List<Double> tabelaProdPrecoVenda,
                List<Integer> tabelaProdQtdEstoque, List<String> tabelaClienteCpf, List<String> tabelaClienteNome) {

            criarArquivoPreCadastrosPadraoSeAusente();
            File arq = new File(arquivoPreCadastros);
            if (!arq.exists()) return false;

            List<String> fId = new ArrayList<>();
            List<String> fNome = new ArrayList<>();
            List<String> fCpf = new ArrayList<>();
            List<String> fCargo = new ArrayList<>();
            List<String> fSenha = new ArrayList<>();
            List<Double> fSal = new ArrayList<>();
            List<Boolean> fPago = new ArrayList<>();
            List<String> fStatus = new ArrayList<>();
            List<String> fMotivo = new ArrayList<>();

            List<String> fnRazao = new ArrayList<>();
            List<String> fnCnpj = new ArrayList<>();
            List<String> fnUf = new ArrayList<>();
            List<String> fnProd = new ArrayList<>();
            List<Double> fnVal = new ArrayList<>();
            List<Double> fnMarg = new ArrayList<>();
            List<Boolean> fnPaga = new ArrayList<>();

            List<String> pNome = new ArrayList<>();
            List<String> pNcm = new ArrayList<>();
            List<Double> pPreco = new ArrayList<>();
            List<Integer> pQtd = new ArrayList<>();

            List<String> cCpf = new ArrayList<>();
            List<String> cNome = new ArrayList<>();

            String secaoAtual = "";
            try (BufferedReader br = new BufferedReader(new FileReader(arq))) {
                String linha;
                while ((linha = br.readLine()) != null) {
                    String limpa = linha.trim();
                    if (limpa.isEmpty() || limpa.startsWith("#")) continue;
                    if (limpa.startsWith("[") && limpa.endsWith("]")) {
                        secaoAtual = limpa.toUpperCase();
                        continue;
                    }
                    String[] partes = limpa.split(";");
                    if ("[FUNCIONARIOS]".equals(secaoAtual) && partes.length >= 9) {
                        fId.add(partes[0].trim().toUpperCase());
                        fNome.add(partes[1].trim().toUpperCase());
                        fCpf.add(partes[2].trim().replaceAll("[^0-9]", ""));
                        fCargo.add(partes[3].trim().toUpperCase());
                        fSenha.add(partes[4].trim());
                        fSal.add(Double.parseDouble(partes[5].trim().replace(",", ".")));
                        fPago.add(Boolean.parseBoolean(partes[6].trim()));
                        fStatus.add(partes[7].trim().toUpperCase());
                        fMotivo.add(partes[8].trim().toUpperCase());
                    } else if ("[FORNECEDORES]".equals(secaoAtual) && partes.length >= 7) {
                        fnRazao.add(partes[0].trim().toUpperCase());
                        fnCnpj.add(partes[1].trim().replaceAll("[^0-9]", ""));
                        fnUf.add(partes[2].trim().toUpperCase());
                        fnProd.add(partes[3].trim().toUpperCase());
                        fnVal.add(Double.parseDouble(partes[4].trim().replace(",", ".")));
                        fnMarg.add(Double.parseDouble(partes[5].trim().replace(",", ".")));
                        fnPaga.add(Boolean.parseBoolean(partes[6].trim()));
                    } else if ("[PRODUTOS]".equals(secaoAtual) && partes.length >= 4) {
                        pNome.add(partes[0].trim().toUpperCase());
                        pNcm.add(partes[1].trim().toUpperCase());
                        pPreco.add(Double.parseDouble(partes[2].trim().replace(",", ".")));
                        pQtd.add(Integer.parseInt(partes[3].trim()));
                    } else if ("[CLIENTES]".equals(secaoAtual) && partes.length >= 2) {
                        cCpf.add(partes[0].trim().replaceAll("[^0-9]", ""));
                        cNome.add(partes[1].trim().toUpperCase());
                    }
                }
            } catch (IOException | NumberFormatException e) {
                return false;
            }

            if (!fId.isEmpty()) {
                tabelaFuncId.clear(); tabelaFuncId.addAll(fId);
                tabelaFuncNome.clear(); tabelaFuncNome.addAll(fNome);
                tabelaFuncCpf.clear(); tabelaFuncCpf.addAll(fCpf);
                tabelaFuncCargo.clear(); tabelaFuncCargo.addAll(fCargo);
                tabelaFuncSenha.clear(); tabelaFuncSenha.addAll(fSenha);
                tabelaFuncSalario.clear(); tabelaFuncSalario.addAll(fSal);
                tabelaFuncPago.clear(); tabelaFuncPago.addAll(fPago);
                tabelaFuncStatus.clear(); tabelaFuncStatus.addAll(fStatus);
                tabelaFuncMotivoInativo.clear(); tabelaFuncMotivoInativo.addAll(fMotivo);
            }
            if (!fnRazao.isEmpty()) {
                tabelaFornRazaoSocial.clear(); tabelaFornRazaoSocial.addAll(fnRazao);
                tabelaFornCnpj.clear(); tabelaFornCnpj.addAll(fnCnpj);
                tabelaFornUf.clear(); tabelaFornUf.addAll(fnUf);
                tabelaFornProdutoPrincipal.clear(); tabelaFornProdutoPrincipal.addAll(fnProd);
                tabelaFornValorFatura.clear(); tabelaFornValorFatura.addAll(fnVal);
                tabelaFornMargemLucroPct.clear(); tabelaFornMargemLucroPct.addAll(fnMarg);
                tabelaFornFaturaPaga.clear(); tabelaFornFaturaPaga.addAll(fnPaga);
            }
            if (!pNome.isEmpty()) {
                tabelaProdNome.clear(); tabelaProdNome.addAll(pNome);
                tabelaProdNcm.clear(); tabelaProdNcm.addAll(pNcm);
                tabelaProdPrecoVenda.clear(); tabelaProdPrecoVenda.addAll(pPreco);
                tabelaProdQtdEstoque.clear(); tabelaProdQtdEstoque.addAll(pQtd);
            }
            if (!cCpf.isEmpty()) {
                tabelaClienteCpf.clear(); tabelaClienteCpf.addAll(cCpf);
                tabelaClienteNome.clear(); tabelaClienteNome.addAll(cNome);
            }
            return true;
        }

        // Grava o arquivo de backup editável com funcionários (incluindo senha), produtos e caixa
        boolean salvarBackupEditavel(
                List<String> tabelaFuncId, List<String> tabelaFuncNome, List<String> tabelaFuncCpf,
                List<String> tabelaFuncCargo, List<String> tabelaFuncSenha, List<Double> tabelaFuncSalario,
                List<Boolean> tabelaFuncPago, List<String> tabelaFuncStatus, List<String> tabelaFuncMotivoInativo,
                List<String> tabelaProdNome, List<String> tabelaProdNcm, List<Double> tabelaProdPrecoVenda,
                List<Integer> tabelaProdQtdEstoque,
                ModuloCaixa caixa) {

            try (BufferedWriter bw = new BufferedWriter(new FileWriter(arquivoBackupOficial))) {
                bw.write("# Backup editável do sistema - " + LocalDateTime.now().format(FORMATADOR_DATA_HORA) + "\n");
                bw.write("[CAIXA_INICIO]\n");
                bw.write("SALDO=" + (caixa != null ? caixa.obterSaldo() : 0.0) + "\n");
                bw.write("[CAIXA_FIM]\n\n");

                bw.write("[FUNCIONARIOS_INICIO]\n");
                for (int i = 0; i < tabelaFuncId.size(); i++) {
                    bw.write(String.join(";",
                            sanitizarCampoTxt(tabelaFuncId.get(i)),
                            sanitizarCampoTxt(tabelaFuncNome.get(i)),
                            sanitizarCampoTxt(tabelaFuncCpf.get(i)),
                            sanitizarCampoTxt(tabelaFuncCargo.get(i)),
                            sanitizarCampoTxt(tabelaFuncSenha.get(i)),
                            String.valueOf(tabelaFuncSalario.get(i)),
                            String.valueOf(tabelaFuncPago.get(i)),
                            sanitizarCampoTxt(tabelaFuncStatus.get(i)),
                            sanitizarCampoTxt(tabelaFuncMotivoInativo.get(i))) + "\n");
                }
                bw.write("[FUNCIONARIOS_FIM]\n\n");

                bw.write("[PRODUTOS_INICIO]\n");
                for (int i = 0; i < tabelaProdNome.size(); i++) {
                    bw.write(String.join(";",
                            sanitizarCampoTxt(tabelaProdNome.get(i)),
                            sanitizarCampoTxt(tabelaProdNcm.get(i)),
                            String.valueOf(tabelaProdPrecoVenda.get(i)),
                            String.valueOf(tabelaProdQtdEstoque.get(i))) + "\n");
                }
                bw.write("[PRODUTOS_FIM]\n");
                return true;
            } catch (IOException e) {
                return false;
            }
        }

        // Exporta o backup com confirmação do usuário e registra saída/auditoria após gravar com sucesso
        void exportarComConfirmacao(
                Scanner leitorEntrada, ModuloSessao sessao, ModuloAuditoria auditoria,
                List<String> tabelaFuncId, List<String> tabelaFuncNome, List<String> tabelaFuncCpf,
                List<String> tabelaFuncCargo, List<String> tabelaFuncSenha, List<Double> tabelaFuncSalario,
                List<Boolean> tabelaFuncPago, List<String> tabelaFuncStatus, List<String> tabelaFuncMotivoInativo,
                List<String> tabelaProdNome, List<String> tabelaProdNcm, List<Double> tabelaProdPrecoVenda,
                List<Integer> tabelaProdQtdEstoque,
                int quantidadeTransacoes,
                ModuloCaixa caixa) {

            if (!sessao.exigirAutoridade(Autoridade.SIS_AUDITORIA_E_BACKUP, "Sem autoridade para exportar backup.")) return;
            System.out.print("Confirma salvar/atualizar '" + arquivoBackupOficial + "'? [S/N]: ");
            if (!lerLinhaSegura(leitorEntrada).toUpperCase().equals("S")) {
                System.out.println(CoresAnsi.aviso(" Exportação cancelada.\n"));
                return;
            }

            boolean gravou = salvarBackupEditavel(
                    tabelaFuncId, tabelaFuncNome, tabelaFuncCpf, tabelaFuncCargo, tabelaFuncSenha,
                    tabelaFuncSalario, tabelaFuncPago, tabelaFuncStatus, tabelaFuncMotivoInativo,
                    tabelaProdNome, tabelaProdNcm, tabelaProdPrecoVenda, tabelaProdQtdEstoque,
                    caixa
            );

            // Registra a saída de dados e a auditoria apenas após confirmar que o backup foi gravado no disco
            if (gravou) {
                caixa.registrarMovimentoDados(
                        "SAIDA_DADOS",
                        "Operador: " + sessao.obterNomeVigente(),
                        "Exportação de backup para '" + arquivoBackupOficial + "' (" + quantidadeTransacoes + " lançamentos)"
                );
                auditoria.registrar(sessao.obterNomeVigente(), "Exportou/atualizou arquivo editável " + arquivoBackupOficial);
                System.out.println(CoresAnsi.sucesso(" Arquivo '" + arquivoBackupOficial + "' salvo com sucesso.\n"));
            } else {
                System.out.println(CoresAnsi.erro("Falha técnica ao gravar arquivo.\n"));
            }
        }

        // Restaura dados a partir do arquivo de backup oficial validando integridade e funcionários ativos
        void restaurarBackupOficial(
                Scanner leitorEntrada, ModuloSessao sessao, ModuloAuditoria auditoria, ModuloAutoridades moduloAutoridades,
                List<String> tabelaFuncId, List<String> tabelaFuncNome, List<String> tabelaFuncCpf,
                List<String> tabelaFuncCargo, List<String> tabelaFuncSenha, List<Double> tabelaFuncSalario,
                List<Boolean> tabelaFuncPago, List<String> tabelaFuncStatus, List<String> tabelaFuncMotivoInativo,
                List<String> tabelaProdNome, List<String> tabelaProdNcm, List<Double> tabelaProdPrecoVenda,
                List<Integer> tabelaProdQtdEstoque,
                ModuloCaixa caixa) {

            if (!sessao.exigirAutoridade(Autoridade.SIS_AUDITORIA_E_BACKUP, "Sem autoridade para restaurar backup.")) return;
            File arq = new File(arquivoBackupOficial);
            if (!arq.exists()) {
                System.out.println(CoresAnsi.aviso(" Arquivo de backup '" + arquivoBackupOficial + "' não encontrado.\n"));
                return;
            }

            System.out.print(CoresAnsi.aviso(" Restaurar '" + arquivoBackupOficial + "' substituirá os dados em memória. Continuar? [S/N]: "));
            if (!lerLinhaSegura(leitorEntrada).toUpperCase().equals("S")) return;

            List<String> tempFuncId = new ArrayList<>();
            List<String> tempFuncNome = new ArrayList<>();
            List<String> tempFuncCpf = new ArrayList<>();
            List<String> tempFuncCargo = new ArrayList<>();
            List<String> tempFuncSenha = new ArrayList<>();
            List<Double> tempFuncSalario = new ArrayList<>();
            List<Boolean> tempFuncPago = new ArrayList<>();
            List<String> tempFuncStatus = new ArrayList<>();
            List<String> tempFuncMotivo = new ArrayList<>();

            List<String> tempProdNome = new ArrayList<>();
            List<String> tempProdNcm = new ArrayList<>();
            List<Double> tempProdPreco = new ArrayList<>();
            List<Integer> tempProdQtd = new ArrayList<>();

            double novoSaldo = caixa.obterSaldo();
            String secao = "";

            try (BufferedReader br = new BufferedReader(new FileReader(arq))) {
                String linha;
                while ((linha = br.readLine()) != null) {
                    String limpa = linha.trim();
                    if (limpa.isEmpty() || limpa.startsWith("#")) continue;
                    if (limpa.startsWith("[") && limpa.endsWith("]")) {
                        secao = limpa.toUpperCase();
                        continue;
                    }
                    if ("[CAIXA_INICIO]".equals(secao) && limpa.startsWith("SALDO=")) {
                        novoSaldo = Double.parseDouble(limpa.substring(6).trim().replace(",", "."));
                    } else if ("[FUNCIONARIOS_INICIO]".equals(secao)) {
                        String[] p = limpa.split(";");
                        if (p.length >= 9) {
                            tempFuncId.add(p[0].trim().toUpperCase());
                            tempFuncNome.add(p[1].trim().toUpperCase());
                            tempFuncCpf.add(p[2].trim().replaceAll("[^0-9]", ""));
                            tempFuncCargo.add(p[3].trim().toUpperCase());
                            tempFuncSenha.add(p[4].trim());
                            tempFuncSalario.add(Double.parseDouble(p[5].trim().replace(",", ".")));
                            tempFuncPago.add(Boolean.parseBoolean(p[6].trim()));
                            tempFuncStatus.add(p[7].trim().toUpperCase());
                            tempFuncMotivo.add(p[8].trim().toUpperCase());
                        }
                    } else if ("[PRODUTOS_INICIO]".equals(secao)) {
                        String[] p = limpa.split(";");
                        if (p.length >= 4) {
                            tempProdNome.add(p[0].trim().toUpperCase());
                            tempProdNcm.add(p[1].trim().toUpperCase());
                            tempProdPreco.add(Double.parseDouble(p[2].trim().replace(",", ".")));
                            tempProdQtd.add(Integer.parseInt(p[3].trim()));
                        }
                    }
                }

                // Evita restaurar um arquivo de backup sem nenhum funcionário ativo, o que bloquearia o acesso ao sistema
                boolean existeFuncionarioAtivo = false;
                for (String st : tempFuncStatus) {
                    if ("ATIVO".equalsIgnoreCase(st)) {
                        existeFuncionarioAtivo = true;
                        break;
                    }
                }
                if (tempFuncId.isEmpty() || !existeFuncionarioAtivo) {
                    throw new IllegalArgumentException("O arquivo de backup deve conter ao menos um funcionário ATIVO.");
                }

                tabelaFuncId.clear(); tabelaFuncId.addAll(tempFuncId);
                tabelaFuncNome.clear(); tabelaFuncNome.addAll(tempFuncNome);
                tabelaFuncCpf.clear(); tabelaFuncCpf.addAll(tempFuncCpf);
                tabelaFuncCargo.clear(); tabelaFuncCargo.addAll(tempFuncCargo);
                tabelaFuncSenha.clear(); tabelaFuncSenha.addAll(tempFuncSenha);
                tabelaFuncSalario.clear(); tabelaFuncSalario.addAll(tempFuncSalario);
                tabelaFuncPago.clear(); tabelaFuncPago.addAll(tempFuncPago);
                tabelaFuncStatus.clear(); tabelaFuncStatus.addAll(tempFuncStatus);
                tabelaFuncMotivoInativo.clear(); tabelaFuncMotivoInativo.addAll(tempFuncMotivo);

                if (!tempProdNome.isEmpty()) {
                    tabelaProdNome.clear(); tabelaProdNome.addAll(tempProdNome);
                    tabelaProdNcm.clear(); tabelaProdNcm.addAll(tempProdNcm);
                    tabelaProdPrecoVenda.clear(); tabelaProdPrecoVenda.addAll(tempProdPreco);
                    tabelaProdQtdEstoque.clear(); tabelaProdQtdEstoque.addAll(tempProdQtd);
                }

                caixa.definirSaldo(novoSaldo);
                if (moduloAutoridades != null) {
                    moduloAutoridades.sincronizarComTabelaFuncionarios(tabelaFuncId, tabelaFuncCargo, tabelaFuncStatus);
                }
                caixa.registrarMovimentoDados("ENTRADA_DADOS", "Operador: " + sessao.obterNomeVigente(), "Restauração do backup '" + arquivoBackupOficial + "'");
                auditoria.registrar(sessao.obterNomeVigente(), "Restaurou backup oficial " + arquivoBackupOficial);
                System.out.println(CoresAnsi.sucesso(" Backup restaurado com sucesso.\n"));
                // Captura IOException para falhas de arquivo e IllegalArgumentException (que já inclui NumberFormatException) para formatos inválidos
            } catch (IOException | IllegalArgumentException e) {
                System.out.println(CoresAnsi.erro("Falha ao restaurar backup: " + e.getMessage() + "\n"));
            }
        }
    }

    // =========================================================================
    // MÓDULO 6: VALIDAÇÃO DE ENTRADAS E CÁLCULOS FISCAIS / FINANCEIROS
    // Funções puras ou de I/O isolado para leitura segura de console, validação
    // de CPF/CNPJ/UF/números e cálculo de ICMS sem acoplamento de estado.
    // =========================================================================

    // Lê linha do Scanner com segurança e remove espaços das extremidades
    private static String lerLinhaSegura(Scanner leitorEntrada) {
        if (!leitorEntrada.hasNextLine()) return "";
        return leitorEntrada.nextLine().trim();
    }

    // Verifica se o comando digitado representa cancelamento ou retorno ("0", "SAIR" ou "VOLTAR")
    private static boolean isComandoCancelamento(String entrada) {
        if (entrada == null) return false;
        String up = entrada.trim().toUpperCase();
        return up.equals("0") || up.equals("SAIR") || up.equals("VOLTAR");
    }

    // Sanitiza caracteres separadores ou quebras de linha em textos de log/arquivo
    private static String sanitizarCampoTxt(String texto) {
        if (texto == null) return "";
        return texto.replaceAll("[\\r\\n]+", " ").replace(";", ",");
    }

    // Verifica se todos os caracteres de uma sequência numérica são iguais (ex.: "11111111111")
    private static boolean todosDigitosIguais(String digitos) {
        if (digitos == null || digitos.isEmpty()) return true;
        char primeiro = digitos.charAt(0);
        for (int i = 1; i < digitos.length(); i++) {
            if (digitos.charAt(i) != primeiro) return false;
        }
        return true;
    }

    // Valida CPF de 11 dígitos pelos dois dígitos verificadores
    private static boolean isCpfValido(String cpfSomenteNumeros) {
        if (cpfSomenteNumeros == null || !cpfSomenteNumeros.matches("^[0-9]{11}$") || todosDigitosIguais(cpfSomenteNumeros)) {
            return false;
        }
        int soma1 = 0;
        for (int i = 0; i < 9; i++) {
            soma1 += (cpfSomenteNumeros.charAt(i) - '0') * (10 - i);
        }
        int resto1 = (soma1 * 10) % 11;
        int dv1 = (resto1 == 10) ? 0 : resto1;

        int soma2 = 0;
        for (int i = 0; i < 10; i++) {
            soma2 += (cpfSomenteNumeros.charAt(i) - '0') * (11 - i);
        }
        int resto2 = (soma2 * 10) % 11;
        int dv2 = (resto2 == 10) ? 0 : resto2;

        return dv1 == (cpfSomenteNumeros.charAt(9) - '0') && dv2 == (cpfSomenteNumeros.charAt(10) - '0');
    }

    // Valida CNPJ de 14 dígitos pelos dois dígitos verificadores
    private static boolean isCnpjValido(String cnpjSomenteNumeros) {
        if (cnpjSomenteNumeros == null || !cnpjSomenteNumeros.matches("^[0-9]{14}$") || todosDigitosIguais(cnpjSomenteNumeros)) {
            return false;
        }
        int[] pesos1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] pesos2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int soma1 = 0;
        for (int i = 0; i < 12; i++) soma1 += (cnpjSomenteNumeros.charAt(i) - '0') * pesos1[i];
        int resto1 = soma1 % 11;
        int dv1 = (resto1 < 2) ? 0 : (11 - resto1);
        int soma2 = 0;
        for (int i = 0; i < 13; i++) soma2 += (cnpjSomenteNumeros.charAt(i) - '0') * pesos2[i];
        int resto2 = soma2 % 11;
        int dv2 = (resto2 < 2) ? 0 : (11 - resto2);
        return dv1 == (cnpjSomenteNumeros.charAt(12) - '0') && dv2 == (cnpjSomenteNumeros.charAt(13) - '0');
    }

    // Formata valores numéricos em moeda brasileira (R$)
    private static String formatarValorMonetarioReais(double valor) {
        if (!Double.isFinite(valor)) return "R$ 0,00";
        return FORMATADOR_MOEDA_BR.format(valor);
    }

    // Formata aviso visual quando o saldo de estoque está zerado ou baixo
    private static String formatarAlertaEstoque(int saldo) {
        if (saldo <= 0) return CoresAnsi.erro(" [SEM ESTOQUE]");
        if (saldo <= 10) return CoresAnsi.aviso(" [ESTOQUE BAIXO]");
        return "";
    }

    // Retorna o salário base padrão conforme o cargo
    private static double obterSalarioBasePorCargo(String cargo) {
        if (cargo == null) return 0.0;
        switch (cargo.trim().toUpperCase()) {
            case "GERENTE":
                return 9500.00;
            case "FINANCEIRO":
                return 6200.00;
            case "OPERADOR":
                return 3800.00;
            default:
                return 0.0;
        }
    }

    // Gera senha temporária de 6 dígitos numéricos
    private static String gerarSenhaTemporaria() {
        int numero = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return String.valueOf(numero);
    }

    // Lê e valida texto contendo apenas letras (com acentos) e espaços
    public static String validarELerTextoAlfabetico(Scanner leitorEntrada, String mensagemPrompt) {
        while (true) {
            System.out.print(mensagemPrompt);
            String textoDigitado = lerLinhaSegura(leitorEntrada);
            if (isComandoCancelamento(textoDigitado)) return "CANCELAR";
            String normalizado = textoDigitado.replaceAll("\\s+", " ").trim();
            if (!normalizado.isEmpty() && normalizado.matches("^[a-zA-ZáàâãéèêíïóôõúüçÁÀÂÃÉÈÊÍÏÓÔÕÚÜÇ ]+$")) {
                return normalizado.toUpperCase();
            }
            System.out.println(CoresAnsi.erro("[Validação]: Digite apenas letras e espaços.\n"));
        }
    }

    // Lê e valida texto livre não vazio que não comece com '-'
    public static String validarELerTextoLivreObrigatorio(Scanner leitorEntrada, String mensagemPrompt) {
        while (true) {
            System.out.print(mensagemPrompt);
            String textoDigitado = lerLinhaSegura(leitorEntrada);
            if (isComandoCancelamento(textoDigitado)) return "CANCELAR";
            String normalizado = textoDigitado.replaceAll("\\s+", " ").trim();
            if (!normalizado.isEmpty() && !normalizado.startsWith("-")) {
                return normalizado.toUpperCase();
            }
            System.out.println(CoresAnsi.erro("[Validação]: Campo obrigatório e não pode iniciar com '-'.\n"));
        }
    }

    // Lê e valida sigla de unidade federativa brasileira (UF) usando constante estática UFS_BRASIL
    public static String validarELerSiglaEstadoUf(Scanner leitorEntrada, String mensagemPrompt) {
        while (true) {
            System.out.print(mensagemPrompt);
            String sigla = lerLinhaSegura(leitorEntrada).toUpperCase();
            if (isComandoCancelamento(sigla)) return "CANCELAR";
            if (UFS_BRASIL.contains(sigla)) return sigla;
            System.out.println(CoresAnsi.erro("[Validação]: UF inválida.\n"));
        }
    }

    // Lê número inteiro positivo (>= 1); retorna -999 se o usuário digitar 0 ou SAIR
    public static int validarELerNumeroInteiro(Scanner leitorEntrada, String mensagemPrompt) {
        while (true) {
            System.out.print(mensagemPrompt);
            String entrada = lerLinhaSegura(leitorEntrada);
            if (entrada.equals("0") || entrada.equalsIgnoreCase("SAIR")) return -999;
            if (entrada.startsWith("-")) {
                System.out.println(CoresAnsi.erro("[Validação]: Valores negativos ou '-0' não são permitidos.\n"));
                continue;
            }
            if (entrada.matches("^[0-9]+$")) {
                try {
                    return Integer.parseInt(entrada);
                } catch (NumberFormatException e) {
                    System.out.println(CoresAnsi.erro("[Validação]: Número fora do limite.\n"));
                }
            } else {
                System.out.println(CoresAnsi.erro("[Validação]: Digite um número inteiro positivo (ou 0 para voltar).\n"));
            }
        }
    }

    // Lê inteiro >= 0 permitindo "0" como valor válido e "SAIR" para cancelar
    public static int validarELerInteiroNaoNegativoComSair(Scanner leitorEntrada, String mensagemPrompt) {
        while (true) {
            System.out.print(mensagemPrompt);
            String entrada = lerLinhaSegura(leitorEntrada);
            if (entrada.equalsIgnoreCase("SAIR")) return -999;
            if (entrada.startsWith("-")) {
                System.out.println(CoresAnsi.erro("[Validação]: Valores negativos ou '-0' não são aceitos.\n"));
                continue;
            }
            if (entrada.matches("^[0-9]+$")) {
                try {
                    return Integer.parseInt(entrada);
                } catch (NumberFormatException e) {
                    System.out.println(CoresAnsi.erro("[Validação]: Número fora do limite.\n"));
                }
            } else {
                System.out.println(CoresAnsi.erro("[Validação]: Digite um número inteiro >= 0 (ou SAIR para cancelar).\n"));
            }
        }
    }

    // Garante que o inteiro lido seja estritamente maior que zero
    public static int validarELerQuantidadePositiva(Scanner leitorEntrada, String mensagemPrompt) {
        while (true) {
            int val = validarELerNumeroInteiro(leitorEntrada, mensagemPrompt);
            if (val == -999) return -999;
            if (val > 0) return val;
            System.out.println(CoresAnsi.erro("[Validação]: A quantidade deve ser maior que zero.\n"));
        }
    }

    // Lê e valida valor decimal monetário (aceitando vírgula ou ponto e até 4 casas decimais; retorna -999.0 em SAIR ou 0 quando permitirZero é false)
    public static double validarELerValorDecimalMonetario(Scanner leitorEntrada, String mensagemPrompt, boolean permitirZero) {
        while (true) {
            System.out.print(mensagemPrompt);
            String bruta = lerLinhaSegura(leitorEntrada);
            if (bruta.equalsIgnoreCase("SAIR")) return -999.0;
            if (bruta.startsWith("-")) {
                System.out.println(CoresAnsi.erro("[Validação]: Valores negativos ou '-0' não são aceitos.\n"));
                continue;
            }
            if (bruta.startsWith("+")) {
                System.out.println(CoresAnsi.erro("[Validação]: Não utilize sinal de '+' antes do valor.\n"));
                continue;
            }
            String norm = bruta.replace(",", ".");
            if (!permitirZero && norm.matches("^0(\\.0+)?$")) return -999.0;
            if (!norm.matches("^[0-9]+(\\.[0-9]{1,4})?$")) {
                System.out.println(CoresAnsi.erro("[Validação]: Informe um valor numérico válido.\n"));
                continue;
            }
            try {
                double v = Double.parseDouble(norm);
                if (Double.isFinite(v) && ((permitirZero && v >= 0.0) || (!permitirZero && v > 0.0))) return v;
            } catch (NumberFormatException ignored) {}
            System.out.println(CoresAnsi.erro("[Validação]: Valor monetário inválido.\n"));
        }
    }

    // Lê CPF, remove pontuação e valida os dígitos verificadores
    public static String validarEFormatarCpf(Scanner leitorEntrada) {
        while (true) {
            System.out.print("Digite o CPF (11 dígitos) [0 para cancelar]: ");
            String entrada = lerLinhaSegura(leitorEntrada);
            if (isComandoCancelamento(entrada)) return "CANCELAR";
            if (entrada.startsWith("-") || !entrada.matches("^[0-9.\\-\\s]+$")) {
                System.out.println(CoresAnsi.erro("[Validação]: CPF inválido.\n"));
                continue;
            }
            String nums = entrada.replaceAll("[^0-9]", "");
            if (isCpfValido(nums)) return nums;
            System.out.println(CoresAnsi.erro("[Validação]: CPF inválido (dígitos verificadores não conferem).\n"));
        }
    }

    // Lê CNPJ, remove pontuação e valida os dígitos verificadores
    public static String validarEFormatarCnpj(Scanner leitorEntrada) {
        while (true) {
            System.out.print("Digite o CNPJ (14 dígitos) [0 para cancelar]: ");
            String entrada = lerLinhaSegura(leitorEntrada);
            if (isComandoCancelamento(entrada)) return "CANCELAR";
            if (entrada.startsWith("-") || !entrada.matches("^[0-9./\\-\\s]+$")) {
                System.out.println(CoresAnsi.erro("[Validação]: CNPJ inválido.\n"));
                continue;
            }
            String nums = entrada.replaceAll("[^0-9]", "");
            if (isCnpjValido(nums)) return nums;
            System.out.println(CoresAnsi.erro("[Validação]: CNPJ inválido (dígitos verificadores não conferem).\n"));
        }
    }

    // Calcula a alíquota de ICMS conforme UFs de origem/destino e exercício fiscal
    public static double calcularAliquotaIcmsPorOperacao(String ufOrigemOperacao, String ufDestinoOperacao, int anoExercicioFiscal) {
        String origem = (ufOrigemOperacao == null) ? "SP" : ufOrigemOperacao.trim().toUpperCase();
        String destino = (ufDestinoOperacao == null) ? origem : ufDestinoOperacao.trim().toUpperCase();
        if (origem.equals(destino)) {
            return "SP".equals(destino) ? 0.18 : 0.17;
        }
        Set<String> sulSudesteExcetoEs = Set.of("MG", "PR", "RJ", "RS", "SC", "SP");
        if (sulSudesteExcetoEs.contains(origem) && !sulSudesteExcetoEs.contains(destino)) {
            return (anoExercicioFiscal >= 2026) ? 0.07 : 0.07;
        }
        return 0.12;
    }

    // Converte a opção numérica do menu no nome oficial do cargo cadastrado
    private static String resolverCargoPorOpcao(int opcaoCargo) {
        switch (opcaoCargo) {
            case 1:
                return "GERENTE";
            case 2:
                return "OPERADOR";
            case 3:
                return "FINANCEIRO";
            default:
                return null;
        }
    }

    // =========================================================================
    // MÓDULO 7: RECURSOS HUMANOS E PERFIL DO USUÁRIO
    // Agrupa operações de cadastro, alteração de cargo/salário, pagamento de
    // folha, inativação/readmissão, autoridades e troca de senha em "Meu Perfil".
    // =========================================================================

    // Altera o cargo de um funcionário ativo (promoção ou rebaixamento), ajusta salário e sincroniza autoridades
    private static void alterarCargoFuncionario(
            Scanner leitorEntrada,
            int indiceFuncionario,
            ModuloSessao sessao,
            ModuloAuditoria auditoria,
            ModuloCaixa caixa,
            ModuloAutoridades moduloAutoridades,
            List<String> tabelaFuncId,
            List<String> tabelaFuncNome,
            List<String> tabelaFuncCargo,
            List<Double> tabelaFuncSalario) {

        // Exige autoridade específica para alterar cargo/salário e impede ação em conta inativa
        if (!sessao.exigirAutoridade(Autoridade.RH_ALTERAR_CARGO_SALARIO, "Sem autoridade para alterar cargo/salário.")) {
            return;
        }
        if (indiceFuncionario < 0 || indiceFuncionario >= tabelaFuncId.size()) {
            System.out.println(CoresAnsi.erro("Registro de funcionário inválido.\n"));
            return;
        }

        String idAlvo = tabelaFuncId.get(indiceFuncionario);
        String nomeAlvo = tabelaFuncNome.get(indiceFuncionario);
        String cargoAtual = tabelaFuncCargo.get(indiceFuncionario).toUpperCase();

        // Exibe opções de cargo e lê a escolha do operador
        int opcaoCargo = validarELerNumeroInteiro(
                leitorEntrada,
                "Novo cargo [1 - GERENTE | 2 - OPERADOR | 3 - FINANCEIRO] [0 para cancelar]: "
        );
        if (opcaoCargo == -999) return;

        String novoCargo = resolverCargoPorOpcao(opcaoCargo);
        if (novoCargo == null) {
            System.out.println(CoresAnsi.erro("Opção de cargo inválida.\n"));
            return;
        }
        if (novoCargo.equalsIgnoreCase(cargoAtual)) {
            System.out.println(CoresAnsi.aviso(" O funcionário já ocupa o cargo de " + cargoAtual + ".\n"));
            return;
        }

        // Calcula novo salário base e atualiza cargo e remuneração na tabela em memória
        double novoSalario = obterSalarioBasePorCargo(novoCargo);
        tabelaFuncCargo.set(indiceFuncionario, novoCargo);
        tabelaFuncSalario.set(indiceFuncionario, novoSalario);

        // Redefine o EnumSet de autoridades de acordo com o novo cargo para o usuário ativo
        if (moduloAutoridades != null) {
            moduloAutoridades.redefinirPorCargo(idAlvo, novoCargo, true);
        }

        // Classifica a movimentação para deixar o registro de auditoria e log mais claro
        String tipoMovimento = "ALTERAÇÃO DE CARGO";
        if (novoSalario > obterSalarioBasePorCargo(cargoAtual)) {
            tipoMovimento = "PROMOÇÃO";
        } else if (novoSalario < obterSalarioBasePorCargo(cargoAtual)) {
            tipoMovimento = "REBAIXAMENTO";
        }

        String detalheEvento = tipoMovimento + " de " + nomeAlvo + " (ID: " + idAlvo + "): "
                + cargoAtual + " -> " + novoCargo + " | Novo salário base: " + formatarValorMonetarioReais(novoSalario);

        caixa.registrarMovimentoDados("ENTRADA_DADOS", "Operador: " + sessao.obterNomeVigente(), detalheEvento);
        auditoria.registrar(sessao.obterNomeVigente(), detalheEvento);
        System.out.println(CoresAnsi.sucesso(" " + detalheEvento + ".\n"));
    }

    // Exibe os dados do usuário logado e permite alterar a própria senha com persistência automática
    public static void exibirAbaMeuPerfil(
            Scanner leitorEntrada,
            ModuloSessao sessao,
            ModuloAuditoria auditoria,
            ModuloCaixa caixa,
            ModuloPersistenciaTxt persistencia,
            List<String> tabelaFuncId, List<String> tabelaFuncNome, List<String> tabelaFuncCpf,
            List<String> tabelaFuncCargo, List<String> tabelaFuncSenha,
            List<Double> tabelaFuncSalario, List<Boolean> tabelaFuncPago,
            List<String> tabelaFuncStatus, List<String> tabelaFuncMotivoInativo,
            List<String> tabelaProdNome, List<String> tabelaProdNcm, List<Double> tabelaProdPrecoVenda,
            List<Integer> tabelaProdQtdEstoque) {

        boolean abaPerfilAtiva = true;
        while (abaPerfilAtiva) {
            if (!sessao.validarOuRevogarSessao()) return;

            // Localiza o registro ativo correspondente ao usuário da sessão
            // Prioriza a correspondência exata do ID da sessão para evitar ambiguidade se houver nomes iguais
            int indice = -1;
            for (int i = 0; i < tabelaFuncId.size(); i++) {
                if (tabelaFuncId.get(i).equalsIgnoreCase(sessao.obterIdVigente())
                        && tabelaFuncStatus.get(i).equalsIgnoreCase("ATIVO")) {
                    indice = i;
                    break;
                }
            }
            if (indice == -1) {
                System.out.println(CoresAnsi.erro("Registro não encontrado.\n"));
                return;
            }

            boolean estaAtivo = tabelaFuncStatus.get(indice).equalsIgnoreCase("ATIVO");
            boolean salarioPago = tabelaFuncPago.get(indice);

            System.out.println(CoresAnsi.titulo("\n┌────────────────────────────────────────────────────────────────┐"));
            System.out.println(CoresAnsi.titulo("│                          MEU PERFIL                          │"));
            System.out.println(CoresAnsi.titulo("├────────────────────────────────────────────────────────────────┤"));
            System.out.printf("│ Matrícula / ID   : %s\n", tabelaFuncId.get(indice));
            System.out.printf("│ Nome Completo    : %s\n", tabelaFuncNome.get(indice));
            System.out.printf("│ CPF              : %s\n", tabelaFuncCpf.get(indice));
            // Exibe o cargo vigente obtido diretamente da sessão autenticada
            System.out.printf("│ Perfil / Cargo   : %s\n", sessao.obterCargoVigente());
            System.out.printf("│ Status da Conta  : %s\n", (estaAtivo ? CoresAnsi.sucesso("ATIVO ") : CoresAnsi.erro("INATIVO ")));
            System.out.printf("│ Autoridade       : %s\n", (estaAtivo ? CoresAnsi.sucesso("HABILITADA") : CoresAnsi.erro("REVOGADA")));
            System.out.printf("│ Salário Base     : %s\n", sessao.exibirValorCondicional(Autoridade.FIN_VER_VALORES_SENSIVEIS, tabelaFuncSalario.get(indice)));
            System.out.printf("│ Situação Folha   : %s\n", (salarioPago ? CoresAnsi.sucesso("PAGO ") : CoresAnsi.aviso("PENDENTE ")));
            if (!estaAtivo) {
                System.out.printf("│ Observação       : %s\n", CoresAnsi.aviso(tabelaFuncMotivoInativo.get(indice)));
            }
            System.out.println(CoresAnsi.titulo("├────────────────────────────────────────────────────────────────┤"));
            System.out.println("│ [1] Alterar Senha                                              │");
            System.out.println("│ [0] Continuar                                                  │");
            System.out.println(CoresAnsi.titulo("└────────────────────────────────────────────────────────────────┘"));

            int acaoPerfil = validarELerNumeroInteiro(leitorEntrada, "Escolha: ");
            if (acaoPerfil == 0 || acaoPerfil == -999) {
                abaPerfilAtiva = false;
            } else if (acaoPerfil == 1) {
                System.out.print(" Senha atual [0 para cancelar]: ");
                String senhaAtualInformada = lerLinhaSegura(leitorEntrada);
                if (isComandoCancelamento(senhaAtualInformada)) continue;

                // Delega ao ModuloSessao a conferência da senha atual contra a tabela paralela de senhas
                if (!sessao.senhaAtualCorresponde(senhaAtualInformada)) {
                    System.out.println(CoresAnsi.erro("Senha atual incorreta.\n"));
                    continue;
                }

                System.out.print(" Nova senha (mínimo 6 caracteres, sem espaços) [0 para cancelar]: ");
                String novaSenha = lerLinhaSegura(leitorEntrada);
                if (isComandoCancelamento(novaSenha)) continue;

                // Valida tamanho mínimo e ausência de espaços na nova senha
                if (novaSenha.length() < 6 || novaSenha.contains(" ")) {
                    System.out.println(CoresAnsi.erro("A nova senha deve ter pelo menos 6 caracteres e não pode conter espaços.\n"));
                    continue;
                }
                if (novaSenha.equals(tabelaFuncSenha.get(indice))) {
                    System.out.println(CoresAnsi.aviso(" A nova senha deve ser diferente da senha atual.\n"));
                    continue;
                }

                System.out.print(" Confirme a nova senha [0 para cancelar]: ");
                String confirmacaoSenha = lerLinhaSegura(leitorEntrada);
                if (isComandoCancelamento(confirmacaoSenha)) continue;

                if (!novaSenha.equals(confirmacaoSenha)) {
                    System.out.println(CoresAnsi.erro("A confirmação não coincide com a nova senha.\n"));
                    continue;
                }

                // Atualiza a senha na tabela paralela em memória imediatamente após a confirmação
                tabelaFuncSenha.set(indice, novaSenha);

                // Registra a alteração no log de entrada/saída de dados e na auditoria da sessão
                String operadorAtual = sessao.obterNomeVigente() + " (" + sessao.obterIdVigente() + ")";
                caixa.registrarMovimentoDados("ENTRADA_DADOS", operadorAtual, "Atualização de senha em Meu Perfil (" + tabelaFuncId.get(indice) + ")");
                auditoria.registrar(operadorAtual, "Alterou a própria senha em Meu Perfil");

                // Persiste a senha atualizada automaticamente no arquivo de backup editável (.TXT)
                boolean salvouTxt = persistencia.salvarBackupEditavel(
                        tabelaFuncId, tabelaFuncNome, tabelaFuncCpf, tabelaFuncCargo, tabelaFuncSenha,
                        tabelaFuncSalario, tabelaFuncPago, tabelaFuncStatus, tabelaFuncMotivoInativo,
                        tabelaProdNome, tabelaProdNcm, tabelaProdPrecoVenda, tabelaProdQtdEstoque,
                        caixa
                );

                // Informa ao usuário se a gravação no arquivo de backup foi concluída ou falhou
                if (salvouTxt) {
                    System.out.println(CoresAnsi.sucesso(" Senha alterada e salva automaticamente em '" + persistencia.obterNomeArquivoBackup() + "'.\n"));
                } else {
                    System.out.println(CoresAnsi.aviso(" Senha alterada na sessão, mas houve falha ao gravar '" + persistencia.obterNomeArquivoBackup() + "'.\n"));
                }
            } else {
                System.out.println(CoresAnsi.erro("Opção inválida.\n"));
            }
        }
    }

    // Auxiliar de cadastro de novo funcionário (chamado diretamente da tabela unificada de RH)
    private static void cadastrarNovoFuncionarioNaTabela(
            Scanner leitorEntrada,
            ModuloSessao sessao,
            ModuloAuditoria auditoria,
            ModuloCaixa caixa,
            ModuloAutoridades moduloAutoridades,
            List<String> tabelaFuncId, List<String> tabelaFuncNome, List<String> tabelaFuncCpf,
            List<String> tabelaFuncCargo, List<String> tabelaFuncSenha, List<Double> tabelaFuncSalario,
            List<Boolean> tabelaFuncPago, List<String> tabelaFuncStatus, List<String> tabelaFuncMotivoInativo) {

        if (!sessao.exigirAutoridade(Autoridade.RH_GERIR_CADASTROS, "Sem autoridade para cadastrar funcionários.")) return;

        String novoIdFuncional;
        while (true) {
            System.out.print("Novo ID (3 dígitos, ex: 009) [0 para cancelar]: ");
            novoIdFuncional = lerLinhaSegura(leitorEntrada).toUpperCase();
            if (isComandoCancelamento(novoIdFuncional)) break;
            if (novoIdFuncional.matches("^[0-9]{3}$") && !novoIdFuncional.equals("000") && !tabelaFuncId.contains(novoIdFuncional)) break;
            System.out.println(CoresAnsi.erro("ID inválido ou já cadastrado.\n"));
        }
        if (isComandoCancelamento(novoIdFuncional)) return;

        String nomeCompleto = validarELerTextoAlfabetico(leitorEntrada, "Nome completo: ");
        if (nomeCompleto.equals("CANCELAR")) return;

        String cpfValidado = validarEFormatarCpf(leitorEntrada);
        if (cpfValidado.equals("CANCELAR")) return;
        if (tabelaFuncCpf.contains(cpfValidado)) {
            System.out.println(CoresAnsi.erro("[Duplicidade]: Já existe um registro com este CPF.\n"));
            return;
        }

        int opcaoCargo = validarELerNumeroInteiro(leitorEntrada, "Nível de acesso [1 - Gerente | 2 - Operador | 3 - Financeiro] [0 para cancelar]: ");
        if (opcaoCargo == -999) return;
        String cargoDefinido = resolverCargoPorOpcao(opcaoCargo);
        if (cargoDefinido == null) {
            System.out.println(CoresAnsi.erro("Opção inválida.\n"));
            return;
        }
        String senhaGerada = gerarSenhaTemporaria();

        tabelaFuncId.add(novoIdFuncional);
        tabelaFuncNome.add(nomeCompleto);
        tabelaFuncCpf.add(cpfValidado);
        tabelaFuncCargo.add(cargoDefinido);
        tabelaFuncSenha.add(senhaGerada);
        tabelaFuncSalario.add(obterSalarioBasePorCargo(cargoDefinido));
        tabelaFuncPago.add(false);
        tabelaFuncStatus.add("ATIVO");
        tabelaFuncMotivoInativo.add("NENHUM");

        if (moduloAutoridades != null) {
            moduloAutoridades.redefinirPorCargo(novoIdFuncional, cargoDefinido, true);
        }

        caixa.registrarMovimentoDados("ENTRADA_DADOS", "Operador: " + sessao.obterNomeVigente(), "Novo cadastro ID " + novoIdFuncional + " - " + nomeCompleto + " (" + cargoDefinido + ")");
        auditoria.registrar(sessao.obterNomeVigente(), "Cadastrou registro ID " + novoIdFuncional + " (" + nomeCompleto + ")");
        System.out.println(CoresAnsi.sucesso(" Registro salvo! Senha inicial gerada: " + senhaGerada + "\n"));
    }

    // Menu unificado de RH: exibe diretamente a tabela paginada/filtrável e permite cadastrar novo sem menu intermediário
    public static void exibirMenuGestaoFuncionarios(
            Scanner leitorEntrada,
            ModuloSessao sessao,
            ModuloAuditoria auditoria,
            ModuloCaixa caixa,
            ModuloPersistenciaTxt persistencia,
            ModuloAutoridades moduloAutoridades,
            List<String> tabelaFuncId, List<String> tabelaFuncNome, List<String> tabelaFuncCpf,
            List<String> tabelaFuncCargo, List<String> tabelaFuncSenha, List<Double> tabelaFuncSalario,
            List<Boolean> tabelaFuncPago, List<String> tabelaFuncStatus, List<String> tabelaFuncMotivoInativo,
            List<String> tabelaFornRazaoSocial, List<String> tabelaFornCnpj, List<String> tabelaFornUf,
            List<String> tabelaFornProdutoPrincipal, List<Double> tabelaFornValorFatura,
            List<Double> tabelaFornMargemLucroPct, List<Boolean> tabelaFornFaturaPaga,
            List<String> tabelaProdNome, List<String> tabelaProdNcm, List<Double> tabelaProdPrecoVenda, List<Integer> tabelaProdQtdEstoque,
            List<String> tabelaClienteCpf, List<String> tabelaClienteNome,
            List<Double> indicadorTotalSalariosPagos) {

        if (!sessao.exigirAutoridade(Autoridade.RH_VER_FUNCIONARIOS, "Sem autoridade para acessar a gestão de funcionários.")) {
            return;
        }

        garantirPreCadastrosAtualizadosDoTxt(
                persistencia, caixa, moduloAutoridades,
                tabelaFuncId, tabelaFuncNome, tabelaFuncCpf,
                tabelaFuncCargo, tabelaFuncSenha, tabelaFuncSalario,
                tabelaFuncPago, tabelaFuncStatus, tabelaFuncMotivoInativo,
                tabelaFornRazaoSocial, tabelaFornCnpj, tabelaFornUf,
                tabelaFornProdutoPrincipal, tabelaFornValorFatura,
                tabelaFornMargemLucroPct, tabelaFornFaturaPaga,
                tabelaProdNome, tabelaProdNcm, tabelaProdPrecoVenda,
                tabelaProdQtdEstoque, tabelaClienteCpf, tabelaClienteNome
        );

        int paginaAtual = 1;
        final int ITENS_POR_PAGINA = 25;
        String filtroFuncAtivo = "";
        boolean navegandoLista = true;

        while (navegandoLista) {
            if (!sessao.validarOuRevogarSessao()) return;

            // Filtra funcionários por ID, nome, CPF, cargo ou status
            List<Integer> indicesFiltrados = new ArrayList<>();
            for (int i = 0; i < tabelaFuncNome.size(); i++) {
                String busca = (tabelaFuncId.get(i) + " " + tabelaFuncNome.get(i) + " "
                        + tabelaFuncCpf.get(i) + " " + tabelaFuncCargo.get(i) + " "
                        + tabelaFuncStatus.get(i)).toUpperCase();
                if (filtroFuncAtivo.isEmpty() || busca.contains(filtroFuncAtivo)) {
                    indicesFiltrados.add(i);
                }
            }

            int totalFiltrados = indicesFiltrados.size();
            int totalPaginas = Math.max(1, (int) Math.ceil((double) totalFiltrados / ITENS_POR_PAGINA));
            if (paginaAtual > totalPaginas) paginaAtual = totalPaginas;

            String etiquetaFiltro = filtroFuncAtivo.isEmpty() ? "TODOS" : "FILTRO: '" + filtroFuncAtivo + "'";
            System.out.println(CoresAnsi.titulo("\n================================================================"));
            System.out.println(CoresAnsi.titulo("                      GESTÃO DE CADASTROS (RH)                  "));
            System.out.println(CoresAnsi.titulo("================================================================"));
            System.out.println(CoresAnsi.titulo("--- REGISTROS [" + etiquetaFiltro + "] (Página " + paginaAtual + " de " + totalPaginas + " | Encontrados: " + totalFiltrados + ") ---"));

            if (tabelaFuncNome.isEmpty()) {
                System.out.println(CoresAnsi.aviso(" Nenhum registro disponível."));
            } else if (totalFiltrados == 0) {
                System.out.println(CoresAnsi.aviso(" Nenhum funcionário encontrado para o filtro '" + filtroFuncAtivo + "'. Use [L] para limpar."));
            } else {
                int indiceInicial = (paginaAtual - 1) * ITENS_POR_PAGINA;
                int indiceFinal = Math.min(indiceInicial + ITENS_POR_PAGINA, totalFiltrados);

                for (int i = indiceInicial; i < indiceFinal; i++) {
                    int idxReal = indicesFiltrados.get(i);
                    String etiquetaPagamento = tabelaFuncPago.get(idxReal) ? CoresAnsi.sucesso("PAGO ") : CoresAnsi.aviso("PENDENTE ");
                    String etiquetaStatus = tabelaFuncStatus.get(idxReal).equalsIgnoreCase("ATIVO")
                            ? CoresAnsi.sucesso("ATIVO")
                            : CoresAnsi.erro("INATIVO (READMITÍVEL) ");
                    String valorExibido = sessao.exibirValorCondicional(Autoridade.FIN_VER_VALORES_SENSIVEIS, tabelaFuncSalario.get(idxReal));
                    System.out.printf("[%d] ID: %s | Nome: %s | Cargo: %s | Status: %s | Valor: %s [%s]\n",
                            (i + 1), tabelaFuncId.get(idxReal), tabelaFuncNome.get(idxReal),
                            tabelaFuncCargo.get(idxReal), etiquetaStatus, valorExibido, etiquetaPagamento);
                }
            }
            System.out.println("----------------------------------------------------------------");
            System.out.print("[N] Próxima | [P] Anterior | [F] Filtrar | [L] Limpar");
            if (sessao.temAutoridade(Autoridade.RH_GERIR_CADASTROS)) {
                System.out.print(" | [C] Cadastrar Novo");
            }
            System.out.println(" | [NÚMERO] Detalhes | [0] Voltar");
            System.out.print("Comando: ");
            String comandoUsuario = lerLinhaSegura(leitorEntrada).toUpperCase();

            if (comandoUsuario.equals("N") && paginaAtual < totalPaginas) paginaAtual++;
            else if (comandoUsuario.equals("P") && paginaAtual > 1) paginaAtual--;
            else if (comandoUsuario.equals("F")) {
                String termo = validarELerTextoLivreObrigatorio(leitorEntrada, "Filtrar por ID, nome, cargo ou status [0 para cancelar]: ");
                if (!termo.equals("CANCELAR")) {
                    filtroFuncAtivo = termo;
                    paginaAtual = 1;
                }
            } else if (comandoUsuario.equals("L")) {
                filtroFuncAtivo = "";
                paginaAtual = 1;
                System.out.println(CoresAnsi.sucesso(" Filtro de funcionários removido.\n"));
            } else if (comandoUsuario.equals("C")) {
                cadastrarNovoFuncionarioNaTabela(
                        leitorEntrada, sessao, auditoria, caixa, moduloAutoridades,
                        tabelaFuncId, tabelaFuncNome, tabelaFuncCpf, tabelaFuncCargo, tabelaFuncSenha,
                        tabelaFuncSalario, tabelaFuncPago, tabelaFuncStatus, tabelaFuncMotivoInativo
                );
            } else if (isComandoCancelamento(comandoUsuario)) navegandoLista = false;
            else if (comandoUsuario.matches("^[0-9]+$")) {
                int pos = Integer.parseInt(comandoUsuario) - 1;
                if (pos >= 0 && pos < totalFiltrados) {
                    int indiceSelecionado = indicesFiltrados.get(pos);
                    abrirModalDetalhesEGestaoFuncionario(
                            leitorEntrada, indiceSelecionado, sessao, auditoria, caixa, moduloAutoridades,
                            tabelaFuncId, tabelaFuncNome, tabelaFuncCpf, tabelaFuncCargo, tabelaFuncSenha, tabelaFuncSalario,
                            tabelaFuncPago, tabelaFuncStatus, tabelaFuncMotivoInativo, indicadorTotalSalariosPagos
                    );
                } else {
                    System.out.println(CoresAnsi.erro("Índice inválido.\n"));
                }
            } else {
                System.out.println(CoresAnsi.erro("Comando inválido.\n"));
            }
        }
    }

    // Permite alternar individualmente (ON/OFF) as autoridades de um funcionário ativo
    private static void gerenciarAutoridadesDoFuncionario(
            Scanner leitorEntrada,
            ModuloSessao sessao,
            ModuloAuditoria auditoria,
            ModuloCaixa caixa,
            ModuloAutoridades moduloAutoridades,
            String idAlvo,
            String nomeAlvo,
            boolean ativoAlvo) {

        if (!ativoAlvo) {
            System.out.println(CoresAnsi.erro(" Não é possível alterar autoridades de um registro inativo.\n"));
            return;
        }

        boolean gerindo = true;
        while (gerindo) {
            EnumSet<Autoridade> atuais = moduloAutoridades.obterAutoridades(idAlvo, true);
            Autoridade[] todas = Autoridade.values();

            System.out.println(CoresAnsi.titulo("\n--- AUTORIDADES DE: " + nomeAlvo + " (ID: " + idAlvo + ") ---"));
            for (int i = 0; i < todas.length; i++) {
                Autoridade aut = todas[i];
                boolean marcada = atuais.contains(aut);
                System.out.printf("[%2d] %s %-28s -> %s\n",
                        (i + 1),
                        (marcada ? CoresAnsi.sucesso("[ON ]") : CoresAnsi.aviso("[OFF]")),
                        aut.name(),
                        aut.getDescricao());
            }
            System.out.println("----------------------------------------------------------------");
            System.out.println("Digite o número da autoridade para alternar ON/OFF [0 para voltar]: ");
            int escolha = validarELerNumeroInteiro(leitorEntrada, "Autoridade: ");
            if (escolha == 0 || escolha == -999) {
                gerindo = false;
            } else if (escolha >= 1 && escolha <= todas.length) {
                Autoridade escolhida = todas[escolha - 1];
                boolean novoEstado = !atuais.contains(escolhida);
                moduloAutoridades.definirAutoridade(idAlvo, escolhida, novoEstado);
                String acaoTexto = (novoEstado ? "Concedeu" : "Revogou") + " autoridade " + escolhida.name() + " para " + nomeAlvo;
                caixa.registrarMovimentoDados("ENTRADA_DADOS", "Operador: " + sessao.obterNomeVigente(), acaoTexto);
                auditoria.registrar(sessao.obterNomeVigente(), acaoTexto);
                System.out.println(CoresAnsi.sucesso(" " + acaoTexto + ".\n"));
            } else {
                System.out.println(CoresAnsi.erro("Opção inválida.\n"));
            }
        }
    }

    // Exibe detalhes de um funcionário e ações de pagamento, edição, inativação, readmissão, senha, autoridades e cargo
    private static void abrirModalDetalhesEGestaoFuncionario(
            Scanner leitorEntrada, int indiceFuncionario,
            ModuloSessao sessao, ModuloAuditoria auditoria, ModuloCaixa caixa, ModuloAutoridades moduloAutoridades,
            List<String> tabelaFuncId, List<String> tabelaFuncNome, List<String> tabelaFuncCpf,
            List<String> tabelaFuncCargo, List<String> tabelaFuncSenha, List<Double> tabelaFuncSalario,
            List<Boolean> tabelaFuncPago, List<String> tabelaFuncStatus, List<String> tabelaFuncMotivoInativo,
            List<Double> indicadorTotalSalariosPagos) {

        boolean podePagar = sessao.temAutoridade(Autoridade.FIN_PAGAR_E_ESTORNAR);
        boolean podeGerirCadastro = sessao.temAutoridade(Autoridade.RH_GERIR_CADASTROS);
        boolean podeResetarSenha = sessao.temAutoridade(Autoridade.RH_RESETAR_SENHA);
        boolean podeGerirAutoridades = sessao.temAutoridade(Autoridade.RH_GERIR_AUTORIDADES);
        // Verifica previamente se o usuário autenticado pode alterar cargo e salário
        boolean podeAlterarCargo = sessao.temAutoridade(Autoridade.RH_ALTERAR_CARGO_SALARIO);

        boolean modalAberto = true;
        while (modalAberto) {
            boolean estaAtivo = tabelaFuncStatus.get(indiceFuncionario).equalsIgnoreCase("ATIVO");
            String idAlvo = tabelaFuncId.get(indiceFuncionario);
            String valorExibido = sessao.exibirValorCondicional(Autoridade.FIN_VER_VALORES_SENSIVEIS, tabelaFuncSalario.get(indiceFuncionario));

            System.out.println(CoresAnsi.titulo("\n┌────────────────────────────────────────────────────────────────┐"));
            System.out.println(CoresAnsi.titulo("│                      [DETALHES DO REGISTRO]                    │"));
            System.out.println(CoresAnsi.titulo("├────────────────────────────────────────────────────────────────┤"));
            System.out.printf("│ Nome: %s | ID: %s | CPF: %s\n", tabelaFuncNome.get(indiceFuncionario), idAlvo, tabelaFuncCpf.get(indiceFuncionario));
            System.out.printf("│ Cargo: %s | Status: %s\n", tabelaFuncCargo.get(indiceFuncionario), (estaAtivo ? CoresAnsi.sucesso("ATIVO") : CoresAnsi.erro("INATIVO")));
            System.out.printf("│ Valor: %s | Situação: %s\n",
                    valorExibido,
                    (tabelaFuncPago.get(indiceFuncionario) ? CoresAnsi.sucesso("PAGO") : CoresAnsi.aviso("PENDENTE")));
            if (!estaAtivo) {
                System.out.printf("│ Motivo Inativação: %s\n", CoresAnsi.aviso(tabelaFuncMotivoInativo.get(indiceFuncionario)));
            }
            System.out.println(CoresAnsi.titulo("├────────────────────────────────────────────────────────────────┤"));
            System.out.println("│ [1] Ver ficha completa                                         │");
            if (podePagar && estaAtivo && !tabelaFuncPago.get(indiceFuncionario)) {
                System.out.println("│ [2] Registrar pagamento                                        │");
            }
            if (podeGerirCadastro && estaAtivo) {
                System.out.println("│ [3] Editar nome                                                │");
                System.out.println("│ [4] Inativar / Desligar                                        │");
            }
            if (podeGerirCadastro && !estaAtivo) {
                System.out.println("│ [5] Readmitir / Reativar                                       │");
            }
            if (podeResetarSenha && estaAtivo) {
                System.out.println("│ [6] Resetar senha para temporária                              │");
            }
            if (podeGerirAutoridades && estaAtivo && moduloAutoridades != null) {
                System.out.println("│ [7] Gerenciar lista de autoridades                             │");
            }
            // Exibe a opção de promoção/rebaixamento apenas para contas ativas com permissão
            if (podeAlterarCargo && estaAtivo) {
                System.out.println("│ [8] Promover / Rebaixar / Alterar cargo                        │");
            }
            System.out.println("│ [0] Fechar                                                     │");
            System.out.println(CoresAnsi.titulo("└────────────────────────────────────────────────────────────────┘"));

            int acaoEscolhida = validarELerNumeroInteiro(leitorEntrada, "Ação: ");
            if (acaoEscolhida == 1) {
                System.out.println(CoresAnsi.titulo("\n--- FICHA COMPLETA ---"));
                System.out.println("• Nome            : " + tabelaFuncNome.get(indiceFuncionario));
                System.out.println("• ID              : " + idAlvo);
                System.out.println("• CPF             : " + tabelaFuncCpf.get(indiceFuncionario));
                System.out.println("• Cargo           : " + tabelaFuncCargo.get(indiceFuncionario));
                System.out.println("• Status          : " + (estaAtivo ? CoresAnsi.sucesso("ATIVO") : CoresAnsi.erro("INATIVO")));
                System.out.println("• Autoridade      : " + (estaAtivo ? CoresAnsi.sucesso("HABILITADA") : CoresAnsi.erro("REVOGADA")));
                System.out.println("• Valor           : " + valorExibido);
                if (moduloAutoridades != null) {
                    System.out.println("• Permissões      : " + moduloAutoridades.obterAutoridades(idAlvo, estaAtivo));
                }
                if (!estaAtivo) {
                    System.out.println("• Observação      : " + CoresAnsi.aviso(tabelaFuncMotivoInativo.get(indiceFuncionario)));
                }
                System.out.println("----------------------------------------------------------------\n");
            } else if (acaoEscolhida == 2) {
                if (!sessao.exigirAutoridade(Autoridade.FIN_PAGAR_E_ESTORNAR, "Sem autoridade para registrar pagamento.")) continue;
                if (!estaAtivo) {
                    System.out.println(CoresAnsi.erro(" Registro inativo.\n"));
                } else if (tabelaFuncPago.get(indiceFuncionario)) {
                    System.out.println(CoresAnsi.aviso(" Pagamento já registrado neste ciclo.\n"));
                } else {
                    double valorSalario = tabelaFuncSalario.get(indiceFuncionario);
                    if (caixa.registrarSaida(valorSalario, "PAGAMENTO", "Funcionário: " + tabelaFuncNome.get(indiceFuncionario), "Quitação de Salário Mensal")) {
                        indicadorTotalSalariosPagos.set(0, indicadorTotalSalariosPagos.get(0) + valorSalario);
                        tabelaFuncPago.set(indiceFuncionario, true);
                        auditoria.registrar(sessao.obterNomeVigente(), "Quitou salário de " + tabelaFuncNome.get(indiceFuncionario));
                        System.out.println(CoresAnsi.sucesso(" Pagamento concluído.\n"));
                    }
                }
            } else if (acaoEscolhida == 3) {
                if (!sessao.exigirAutoridade(Autoridade.RH_GERIR_CADASTROS, "Sem autoridade para editar cadastro.")) continue;
                if (!estaAtivo) {
                    System.out.println(CoresAnsi.erro(" Registro inativo.\n"));
                    continue;
                }
                String novoNome = validarELerTextoAlfabetico(leitorEntrada, "Novo nome completo: ");
                if (!novoNome.equals("CANCELAR")) {
                    tabelaFuncNome.set(indiceFuncionario, novoNome);
                    caixa.registrarMovimentoDados("ENTRADA_DADOS", "Operador: " + sessao.obterNomeVigente(), "Edição de nome do ID " + idAlvo + " para " + novoNome);
                    auditoria.registrar(sessao.obterNomeVigente(), "Editou nome do ID " + idAlvo);
                    System.out.println(CoresAnsi.sucesso(" Nome atualizado.\n"));
                }
            } else if (acaoEscolhida == 4) {
                if (!sessao.exigirAutoridade(Autoridade.RH_GERIR_CADASTROS, "Sem autoridade para inativar registro.")) continue;
                if (!estaAtivo) {
                    System.out.println(CoresAnsi.aviso(" Registro já inativo.\n"));
                    continue;
                }
                String motivo = validarELerTextoLivreObrigatorio(leitorEntrada, "Motivo [0 para cancelar]: ");
                if (motivo.equals("CANCELAR")) continue;

                // Inativa conta, revoga cargo/autoridades e bloqueia a senha
                String nomeAlvo = tabelaFuncNome.get(indiceFuncionario);
                tabelaFuncStatus.set(indiceFuncionario, "DEMITIDO");
                tabelaFuncCargo.set(indiceFuncionario, "SEM CARGO (REVOGADO)");
                tabelaFuncSenha.set(indiceFuncionario, "BLOQUEADA_" + gerarSenhaTemporaria());
                tabelaFuncMotivoInativo.set(indiceFuncionario, motivo);

                if (moduloAutoridades != null) {
                    moduloAutoridades.redefinirPorCargo(idAlvo, "SEM_CARGO", false);
                }

                caixa.registrarMovimentoDados("SAIDA_DADOS", "Operador: " + sessao.obterNomeVigente(), "Inativação de " + nomeAlvo + " (" + motivo + ")");
                auditoria.registrar(sessao.obterNomeVigente(), "Inativou " + nomeAlvo + " e revogou autoridade");
                System.out.println(CoresAnsi.sucesso(" Registro inativado e autoridade revogada.\n"));
                // Se o próprio usuário logado foi inativado, encerra o modal e revoga a sessão
                if (!sessao.estaAtiva()) return;
                modalAberto = false;
            } else if (acaoEscolhida == 5) {
                if (!sessao.exigirAutoridade(Autoridade.RH_GERIR_CADASTROS, "Sem autoridade para readmitir registro.")) continue;
                if (estaAtivo) {
                    System.out.println(CoresAnsi.aviso(" Registro já está ativo.\n"));
                    continue;
                }
                int opcaoCargo = validarELerNumeroInteiro(leitorEntrada, "Nível de acesso [1 - Gerente | 2 - Operador | 3 - Financeiro] [0 para cancelar]: ");
                if (opcaoCargo == -999) continue;
                String novoCargo = resolverCargoPorOpcao(opcaoCargo);
                if (novoCargo == null) {
                    System.out.println(CoresAnsi.erro("Opção inválida.\n"));
                    continue;
                }
                String novaSenha = gerarSenhaTemporaria();

                tabelaFuncStatus.set(indiceFuncionario, "ATIVO");
                tabelaFuncCargo.set(indiceFuncionario, novoCargo);
                tabelaFuncSenha.set(indiceFuncionario, novaSenha);
                tabelaFuncSalario.set(indiceFuncionario, obterSalarioBasePorCargo(novoCargo));
                tabelaFuncPago.set(indiceFuncionario, false);
                tabelaFuncMotivoInativo.set(indiceFuncionario, "READMITIDO");

                if (moduloAutoridades != null) {
                    moduloAutoridades.redefinirPorCargo(idAlvo, novoCargo, true);
                }

                caixa.registrarMovimentoDados("ENTRADA_DADOS", "Operador: " + sessao.obterNomeVigente(), "Readmissão de " + tabelaFuncNome.get(indiceFuncionario) + " (" + novoCargo + ")");
                auditoria.registrar(sessao.obterNomeVigente(), "Readmitiu " + tabelaFuncNome.get(indiceFuncionario));
                System.out.println(CoresAnsi.sucesso(" Registro readmitido. Nova senha gerada: " + novaSenha + "\n"));
            } else if (acaoEscolhida == 6) {
                if (!sessao.exigirAutoridade(Autoridade.RH_RESETAR_SENHA, "Sem autoridade para resetar senha.")) continue;
                if (!estaAtivo) {
                    System.out.println(CoresAnsi.erro(" Registro inativo.\n"));
                    continue;
                }
                System.out.print("Confirma redefinir senha de '" + tabelaFuncNome.get(indiceFuncionario) + "' para '" + gerarSenhaTemporaria() + "'? [S/N]: ");
                if (lerLinhaSegura(leitorEntrada).toUpperCase().equals("S")) {
                    String senhaTemp = gerarSenhaTemporaria();
                    tabelaFuncSenha.set(indiceFuncionario, senhaTemp);
                    caixa.registrarMovimentoDados("ENTRADA_DADOS", "Operador: " + sessao.obterNomeVigente(), "Reset de senha do funcionário ID " + idAlvo);
                    auditoria.registrar(sessao.obterNomeVigente(), "Resetou senha de " + tabelaFuncNome.get(indiceFuncionario));
                    System.out.println(CoresAnsi.sucesso(" Senha redefinida para: " + senhaTemp + "\n"));
                }
            } else if (acaoEscolhida == 7) {
                if (!sessao.exigirAutoridade(Autoridade.RH_GERIR_AUTORIDADES, "Sem autoridade para gerenciar autoridades.")) continue;
                if (moduloAutoridades != null) {
                    gerenciarAutoridadesDoFuncionario(leitorEntrada, sessao, auditoria, caixa, moduloAutoridades, idAlvo, tabelaFuncNome.get(indiceFuncionario), estaAtivo);
                }
            // Trata a ação [8] delegando para alterarCargoFuncionario e revalidando estado ativo e autoridade
            } else if (acaoEscolhida == 8) {
                if (!sessao.exigirAutoridade(Autoridade.RH_ALTERAR_CARGO_SALARIO, "Sem autoridade para alterar cargo/salário.")) continue;
                if (!estaAtivo) {
                    System.out.println(CoresAnsi.erro(" Registro inativo.\n"));
                    continue;
                }
                alterarCargoFuncionario(
                        leitorEntrada, indiceFuncionario, sessao, auditoria, caixa, moduloAutoridades,
                        tabelaFuncId, tabelaFuncNome, tabelaFuncCargo, tabelaFuncSalario
                );
            } else if (acaoEscolhida == 0 || acaoEscolhida == -999) {
                modalAberto = false;
            } else {
                System.out.println(CoresAnsi.erro("Opção indisponível.\n"));
            }
        }
    }

    // =========================================================================
    // MÓDULO 8: FORNECEDORES E ABASTECIMENTO
    // Agrupa cadastro de compras de fornecedores, entrada de estoque, edição,
    // quitação de faturas e remoção de registros de fornecimento.
    // =========================================================================

    // Auxiliar de registro de operação com fornecedor e entrada de estoque
    private static void registrarOperacaoFornecedorNaTabela(
            Scanner leitorEntrada,
            ModuloSessao sessao,
            ModuloAuditoria auditoria,
            ModuloCaixa caixa,
            ModuloMovimentacaoEstoque movEstoque,
            List<String> tabelaFornRazaoSocial, List<String> tabelaFornCnpj, List<String> tabelaFornUf,
            List<String> tabelaFornProdutoPrincipal, List<Double> tabelaFornValorFatura,
            List<Double> tabelaFornMargemLucroPct, List<Boolean> tabelaFornFaturaPaga,
            List<String> tabelaProdNome, List<String> tabelaProdNcm, List<Double> tabelaProdPrecoVenda,
            List<Integer> tabelaProdQtdEstoque,
            List<Double> indicadorGastosEstoque, List<Double> indicadorTotalFornecedoresPagos) {

        if (!sessao.exigirAutoridade(Autoridade.FORN_GERIR_OPERACOES, "Sem autoridade para registrar operação de fornecimento.")) return;

        String razaoSocial = validarELerTextoAlfabetico(leitorEntrada, "Razão Social: ");
        if (razaoSocial.equals("CANCELAR")) return;

        String cnpjValidado = validarEFormatarCnpj(leitorEntrada);
        if (cnpjValidado.equals("CANCELAR")) return;
        if (tabelaFornCnpj.contains(cnpjValidado)) {
            System.out.println(CoresAnsi.erro("[Duplicidade]: Já existe um registro com este CNPJ.\n"));
            return;
        }

        String siglaUf = validarELerSiglaEstadoUf(leitorEntrada, "Estado (UF): ");
        if (siglaUf.equals("CANCELAR")) return;

        String nomeProduto = validarELerTextoLivreObrigatorio(leitorEntrada, "Descrição do item [0 para cancelar]: ");
        if (nomeProduto.equals("CANCELAR")) return;

        double custoUnitario = validarELerValorDecimalMonetario(leitorEntrada, "Custo unitário (R$) [0 para cancelar]: ", false);
        if (custoUnitario <= 0) return;

        int quantidadeCompra = validarELerQuantidadePositiva(leitorEntrada, "Quantidade [0 para cancelar]: ");
        if (quantidadeCompra <= 0) return;

        double valorTotalFatura = custoUnitario * quantidadeCompra;
        if (!Double.isFinite(valorTotalFatura) || valorTotalFatura <= 0.0) {
            System.out.println(CoresAnsi.erro("Valor total inválido.\n"));
            return;
        }

        // Debita do caixa antes de consolidar a compra e a entrada de estoque
        boolean debitou = caixa.registrarSaida(
                valorTotalFatura,
                "COMPRA / ABASTECIMENTO",
                "Fornecedor: " + razaoSocial + " (" + siglaUf + ")",
                "Entrada em estoque: " + quantidadeCompra + "x " + nomeProduto
        );
        if (!debitou) return;

        tabelaFornRazaoSocial.add(razaoSocial);
        tabelaFornCnpj.add(cnpjValidado);
        tabelaFornUf.add(siglaUf);
        tabelaFornProdutoPrincipal.add(nomeProduto);
        tabelaFornValorFatura.add(valorTotalFatura);
        tabelaFornMargemLucroPct.add(40.0);
        tabelaFornFaturaPaga.add(true);

        indicadorGastosEstoque.set(0, indicadorGastosEstoque.get(0) + valorTotalFatura);
        indicadorTotalFornecedoresPagos.set(0, indicadorTotalFornecedoresPagos.get(0) + valorTotalFatura);

        // Soma ao estoque existente ou cria novo item no catálogo com margem padrão de 40%
        int saldoFinalItem;
        int indiceProdutoExistente = tabelaProdNome.indexOf(nomeProduto);
        if (indiceProdutoExistente != -1) {
            saldoFinalItem = tabelaProdQtdEstoque.get(indiceProdutoExistente) + quantidadeCompra;
            tabelaProdQtdEstoque.set(indiceProdutoExistente, saldoFinalItem);
        } else {
            saldoFinalItem = quantidadeCompra;
            tabelaProdNome.add(nomeProduto);
            tabelaProdNcm.add("2106.90.30");
            tabelaProdPrecoVenda.add(custoUnitario * 1.40);
            tabelaProdQtdEstoque.add(saldoFinalItem);
        }

        if (movEstoque != null) {
            movEstoque.registrarMovimento("ENTRADA_FORNECEDOR", nomeProduto, quantidadeCompra, saldoFinalItem, "Fornecedor: " + razaoSocial, sessao.obterNomeVigente());
        }

        auditoria.registrar(sessao.obterNomeVigente(), "Operação registrada junto a " + razaoSocial);
        System.out.println(CoresAnsi.sucesso(" Operação registrada com sucesso.\n"));
    }

    // Menu unificado de fornecedores: exibe diretamente a tabela e permite registrar operação sem menu intermediário
    public static void exibirMenuGestaoFornecedores(
            Scanner leitorEntrada,
            ModuloSessao sessao,
            ModuloAuditoria auditoria,
            ModuloCaixa caixa,
            ModuloPersistenciaTxt persistencia,
            ModuloAutoridades moduloAutoridades,
            ModuloMovimentacaoEstoque movEstoque,
            List<String> tabelaFuncId, List<String> tabelaFuncNome, List<String> tabelaFuncCpf,
            List<String> tabelaFuncCargo, List<String> tabelaFuncSenha, List<Double> tabelaFuncSalario,
            List<Boolean> tabelaFuncPago, List<String> tabelaFuncStatus, List<String> tabelaFuncMotivoInativo,
            List<String> tabelaFornRazaoSocial, List<String> tabelaFornCnpj, List<String> tabelaFornUf,
            List<String> tabelaFornProdutoPrincipal, List<Double> tabelaFornValorFatura,
            List<Double> tabelaFornMargemLucroPct, List<Boolean> tabelaFornFaturaPaga,
            List<String> tabelaProdNome, List<String> tabelaProdNcm, List<Double> tabelaProdPrecoVenda, List<Integer> tabelaProdQtdEstoque,
            List<String> tabelaClienteCpf, List<String> tabelaClienteNome,
            List<Double> indicadorGastosEstoque, List<Double> indicadorTotalFornecedoresPagos) {

        if (!sessao.exigirAutoridade(Autoridade.FORN_VER_LISTA, "Sem autoridade para visualizar fornecedores.")) {
            return;
        }

        garantirPreCadastrosAtualizadosDoTxt(
                persistencia, caixa, moduloAutoridades,
                tabelaFuncId, tabelaFuncNome, tabelaFuncCpf,
                tabelaFuncCargo, tabelaFuncSenha, tabelaFuncSalario,
                tabelaFuncPago, tabelaFuncStatus, tabelaFuncMotivoInativo,
                tabelaFornRazaoSocial, tabelaFornCnpj, tabelaFornUf,
                tabelaFornProdutoPrincipal, tabelaFornValorFatura,
                tabelaFornMargemLucroPct, tabelaFornFaturaPaga,
                tabelaProdNome, tabelaProdNcm, tabelaProdPrecoVenda,
                tabelaProdQtdEstoque, tabelaClienteCpf, tabelaClienteNome
        );

        int paginaAtual = 1;
        final int ITENS_POR_PAGINA = 25;
        boolean navegandoLista = true;

        while (navegandoLista) {
            int totalPaginas = Math.max(1, (int) Math.ceil((double) tabelaFornRazaoSocial.size() / ITENS_POR_PAGINA));
            if (paginaAtual > totalPaginas) paginaAtual = totalPaginas;

            System.out.println(CoresAnsi.titulo("\n================================================================"));
            System.out.println(CoresAnsi.titulo("                   CONTROLE DE FORNECIMENTO                     "));
            System.out.println(CoresAnsi.titulo("================================================================"));
            System.out.println(CoresAnsi.titulo("--- REGISTROS DE FORNECIMENTO (Página " + paginaAtual + " de " + totalPaginas + ") ---"));

            if (tabelaFornRazaoSocial.isEmpty()) {
                System.out.println(CoresAnsi.aviso(" Nenhum registro disponível."));
            } else {
                int indiceInicial = (paginaAtual - 1) * ITENS_POR_PAGINA;
                int indiceFinal = Math.min(indiceInicial + ITENS_POR_PAGINA, tabelaFornRazaoSocial.size());

                for (int i = indiceInicial; i < indiceFinal; i++) {
                    String etiquetaFatura = tabelaFornFaturaPaga.get(i) ? CoresAnsi.sucesso("QUITADO ") : CoresAnsi.aviso("PENDENTE ");
                    String valorExibido = sessao.exibirValorCondicional(Autoridade.FIN_VER_VALORES_SENSIVEIS, tabelaFornValorFatura.get(i));
                    System.out.printf("[%d] %s | CNPJ: %s | UF: %s | Valor: %s [%s]\n",
                            (i + 1), tabelaFornRazaoSocial.get(i), tabelaFornCnpj.get(i), tabelaFornUf.get(i),
                            valorExibido, etiquetaFatura);
                }
            }

            // Barra de comandos unificada de Fornecedores: exibe os itens visíveis e permite registrar operação sem sair da tela
            System.out.println("----------------------------------------------------------------");
            System.out.print("[N] Próxima Página | [P] Página Anterior");
            if (sessao.temAutoridade(Autoridade.FORN_GERIR_OPERACOES)) {
                System.out.print(" | [C] Registrar Operação");
            }
            System.out.println(" | [NÚMERO] Detalhes | [0] Voltar");
            System.out.print("Comando: ");
            String comandoUsuario = lerLinhaSegura(leitorEntrada).toUpperCase();

            if (comandoUsuario.equals("N") && paginaAtual < totalPaginas) paginaAtual++;
            else if (comandoUsuario.equals("P") && paginaAtual > 1) paginaAtual--;
            else if (comandoUsuario.equals("C")) {
                registrarOperacaoFornecedorNaTabela(
                        leitorEntrada, sessao, auditoria, caixa, movEstoque,
                        tabelaFornRazaoSocial, tabelaFornCnpj, tabelaFornUf, tabelaFornProdutoPrincipal,
                        tabelaFornValorFatura, tabelaFornMargemLucroPct, tabelaFornFaturaPaga,
                        tabelaProdNome, tabelaProdNcm, tabelaProdPrecoVenda, tabelaProdQtdEstoque,
                        indicadorGastosEstoque, indicadorTotalFornecedoresPagos
                );
            } else if (isComandoCancelamento(comandoUsuario)) navegandoLista = false;
            else if (comandoUsuario.matches("^[0-9]+$")) {
                int indiceSelecionado = Integer.parseInt(comandoUsuario) - 1;
                if (indiceSelecionado >= 0 && indiceSelecionado < tabelaFornRazaoSocial.size()) {
                    abrirModalDetalhesEGestaoFornecedor(
                            leitorEntrada, indiceSelecionado, sessao, auditoria, caixa,
                            tabelaFornRazaoSocial, tabelaFornCnpj, tabelaFornUf, tabelaFornProdutoPrincipal,
                            tabelaFornValorFatura, tabelaFornMargemLucroPct, tabelaFornFaturaPaga,
                            indicadorTotalFornecedoresPagos
                    );
                } else {
                    System.out.println(CoresAnsi.erro("Índice inválido.\n"));
                }
            } else {
                System.out.println(CoresAnsi.erro("Comando inválido.\n"));
            }
        }
    }

    // Modal de detalhes de um fornecedor com opções de quitar, editar razão social e remover
    private static void abrirModalDetalhesEGestaoFornecedor(
            Scanner leitorEntrada, int indiceFornecedor,
            ModuloSessao sessao, ModuloAuditoria auditoria, ModuloCaixa caixa,
            List<String> tabelaFornRazaoSocial, List<String> tabelaFornCnpj, List<String> tabelaFornUf,
            List<String> tabelaFornProdutoPrincipal, List<Double> tabelaFornValorFatura,
            List<Double> tabelaFornMargemLucroPct, List<Boolean> tabelaFornFaturaPaga,
            List<Double> indicadorTotalFornecedoresPagos) {

        boolean podePagar = sessao.temAutoridade(Autoridade.FIN_PAGAR_E_ESTORNAR);
        boolean podeGerirFornecedor = sessao.temAutoridade(Autoridade.FORN_GERIR_OPERACOES);

        boolean modalAberto = true;
        while (modalAberto) {
            if (indiceFornecedor < 0 || indiceFornecedor >= tabelaFornRazaoSocial.size()) {
                System.out.println(CoresAnsi.aviso(" Registro já removido.\n"));
                return;
            }

            String valorExibido = sessao.exibirValorCondicional(Autoridade.FIN_VER_VALORES_SENSIVEIS, tabelaFornValorFatura.get(indiceFornecedor));

            System.out.println(CoresAnsi.titulo("\n┌────────────────────────────────────────────────────────────────┐"));
            System.out.println(CoresAnsi.titulo("│                    [DETALHES DO FORNECIMENTO]                  │"));
            System.out.println(CoresAnsi.titulo("├────────────────────────────────────────────────────────────────┤"));
            System.out.printf("│ Razão Social: %s\n", tabelaFornRazaoSocial.get(indiceFornecedor));
            System.out.printf("│ CNPJ: %s | UF: %s | Item: %s\n", tabelaFornCnpj.get(indiceFornecedor), tabelaFornUf.get(indiceFornecedor), tabelaFornProdutoPrincipal.get(indiceFornecedor));
            System.out.printf("│ Valor: %s | Situação: %s\n",
                    valorExibido,
                    (tabelaFornFaturaPaga.get(indiceFornecedor) ? CoresAnsi.sucesso("QUITADO") : CoresAnsi.aviso("PENDENTE")));
            System.out.println(CoresAnsi.titulo("├────────────────────────────────────────────────────────────────┤"));
            System.out.println("│ [1] Ver detalhes                                               │");
            if (podePagar && !tabelaFornFaturaPaga.get(indiceFornecedor)) {
                System.out.println("│ [2] Registrar quitação                                         │");
            }
            if (podeGerirFornecedor) {
                System.out.println("│ [3] Editar razão social                                        │");
                System.out.println("│ [4] Remover registro                                           │");
            }
            System.out.println("│ [0] Fechar                                                     │");
            System.out.println(CoresAnsi.titulo("└────────────────────────────────────────────────────────────────┘"));

            int acaoEscolhida = validarELerNumeroInteiro(leitorEntrada, "Ação: ");
            if (acaoEscolhida == 1) {
                System.out.println(CoresAnsi.titulo("\n--- DETALHES DO REGISTRO ---"));
                System.out.println("• Razão Social   : " + tabelaFornRazaoSocial.get(indiceFornecedor));
                System.out.println("• CNPJ           : " + tabelaFornCnpj.get(indiceFornecedor));
                System.out.println("• UF             : " + tabelaFornUf.get(indiceFornecedor));
                System.out.println("• Item           : " + tabelaFornProdutoPrincipal.get(indiceFornecedor));
                System.out.println("• Valor          : " + valorExibido);
                System.out.println("• Margem Padrão  : " + tabelaFornMargemLucroPct.get(indiceFornecedor) + "%");
                System.out.println("----------------------------------------------------------------\n");
            } else if (acaoEscolhida == 2) {
                if (!sessao.exigirAutoridade(Autoridade.FIN_PAGAR_E_ESTORNAR, "Sem autoridade para registrar quitação.")) continue;
                if (tabelaFornFaturaPaga.get(indiceFornecedor)) {
                    System.out.println(CoresAnsi.aviso(" Já consta como quitado.\n"));
                } else {
                    double valorFatura = tabelaFornValorFatura.get(indiceFornecedor);
                    if (caixa.registrarSaida(valorFatura, "PAGAMENTO", "Fornecedor: " + tabelaFornRazaoSocial.get(indiceFornecedor), "Quitação de fatura (" + tabelaFornProdutoPrincipal.get(indiceFornecedor) + ")")) {
                        indicadorTotalFornecedoresPagos.set(0, indicadorTotalFornecedoresPagos.get(0) + valorFatura);
                        tabelaFornFaturaPaga.set(indiceFornecedor, true);
                        auditoria.registrar(sessao.obterNomeVigente(), "Quitou lançamento de " + tabelaFornRazaoSocial.get(indiceFornecedor));
                        System.out.println(CoresAnsi.sucesso(" Quitação registrada.\n"));
                    }
                }
            } else if (acaoEscolhida == 3) {
                if (!sessao.exigirAutoridade(Autoridade.FORN_GERIR_OPERACOES, "Sem autoridade para editar fornecedor.")) continue;
                String novaRazaoSocial = validarELerTextoAlfabetico(leitorEntrada, "Nova Razão Social: ");
                if (!novaRazaoSocial.equals("CANCELAR")) {
                    String antiga = tabelaFornRazaoSocial.get(indiceFornecedor);
                    tabelaFornRazaoSocial.set(indiceFornecedor, novaRazaoSocial);
                    caixa.registrarMovimentoDados("ENTRADA_DADOS", "Operador: " + sessao.obterNomeVigente(), "Razão social alterada de '" + antiga + "' para '" + novaRazaoSocial + "'");
                    System.out.println(CoresAnsi.sucesso(" Razão social atualizada.\n"));
                }
            } else if (acaoEscolhida == 4) {
                if (!sessao.exigirAutoridade(Autoridade.FORN_GERIR_OPERACOES, "Sem autoridade para remover fornecedor.")) continue;
                String nomeRemovido = tabelaFornRazaoSocial.get(indiceFornecedor);
                tabelaFornRazaoSocial.remove(indiceFornecedor);
                tabelaFornCnpj.remove(indiceFornecedor);
                tabelaFornUf.remove(indiceFornecedor);
                tabelaFornProdutoPrincipal.remove(indiceFornecedor);
                tabelaFornValorFatura.remove(indiceFornecedor);
                tabelaFornMargemLucroPct.remove(indiceFornecedor);
                tabelaFornFaturaPaga.remove(indiceFornecedor);
                // Registra a exclusão e encerra o modal para evitar reindexação de índices na mesma tela
                caixa.registrarMovimentoDados("SAIDA_DADOS", "Operador: " + sessao.obterNomeVigente(), "Remoção de registro de fornecimento: " + nomeRemovido);
                auditoria.registrar(sessao.obterNomeVigente(), "Removeu registro: " + nomeRemovido);
                System.out.println(CoresAnsi.aviso(" Registro removido.\n"));
                modalAberto = false;
            } else if (acaoEscolhida == 0 || acaoEscolhida == -999) {
                modalAberto = false;
            } else {
                System.out.println(CoresAnsi.erro("Opção indisponível.\n"));
            }
        }
    }

    // =========================================================================
    // MÓDULO 9: ESTOQUE, RELATÓRIOS FINANCEIROS E LOGS
    // Agrupa catálogo de produtos, movimentações de estoque, histórico de
    // lançamentos, relatório de compras por cliente e visualização/exportação
    // de logs e backups.
    // =========================================================================

    // Consolida vendas por cliente, calcula ticket médio e permite exportar relatório em .TXT
    public static void exibirRelatorioVolumeComprasClientes(
            Scanner leitorEntrada, ModuloAuditoria auditoria, ModuloSessao sessao, ModuloCaixa caixa,
            List<String> tabelaTransacaoTipo, List<String> tabelaTransacaoEnvolvidos,
            List<String> tabelaTransacaoDescricao, List<Double> tabelaTransacaoValor) {

        System.out.println(CoresAnsi.titulo("\n================================================================"));
        System.out.println(CoresAnsi.titulo("               RELATÓRIO DE COMPRAS POR CLIENTE                 "));
        System.out.println(CoresAnsi.titulo("================================================================"));

        System.out.print("Deseja aplicar filtro por palavra-chave? [S/N]: ");
        String opcaoFiltro = lerLinhaSegura(leitorEntrada).toUpperCase();
        String termoFiltro = "";
        if (opcaoFiltro.equals("S")) {
            termoFiltro = validarELerTextoLivreObrigatorio(leitorEntrada, "Digite o termo para filtrar [0 para cancelar]: ");
            if (termoFiltro.equals("CANCELAR")) termoFiltro = "";
        }

        Map<String, Double> totaisPorCliente = new LinkedHashMap<>();
        Map<String, Integer> pedidosPorCliente = new LinkedHashMap<>();

        double totalGeralVendas = 0.0;
        int totalPedidosGeral = 0;

        for (int i = 0; i < tabelaTransacaoTipo.size(); i++) {
            if (tabelaTransacaoTipo.get(i).equalsIgnoreCase("VENDA")) {
                String envolvidos = tabelaTransacaoEnvolvidos.get(i);
                String descricao = tabelaTransacaoDescricao.get(i);
                double valor = tabelaTransacaoValor.get(i);

                if (!termoFiltro.isEmpty() && !envolvidos.toUpperCase().contains(termoFiltro) && !descricao.toUpperCase().contains(termoFiltro)) {
                    continue;
                }

                // Extrai o nome do cliente da string de envolvidos
                // Considera sufixos como "(CPF:" ou "|" quando existirem para isolar o nome com segurança
                String nomeCliente = "CLIENTE AVULSO";
                if (envolvidos.contains("Cliente:")) {
                    int inicio = envolvidos.indexOf("Cliente:") + "Cliente:".length();
                    int fimCpf = envolvidos.indexOf("(CPF:", inicio);
                    int fimPipe = envolvidos.indexOf("|", inicio);
                    int fim = envolvidos.length();
                    if (fimCpf != -1 && fimPipe != -1) fim = Math.min(fimCpf, fimPipe);
                    else if (fimCpf != -1) fim = fimCpf;
                    else if (fimPipe != -1) fim = fimPipe;
                    String extraido = envolvidos.substring(inicio, fim).trim().toUpperCase();
                    if (!extraido.isEmpty()) nomeCliente = extraido;
                }

                totalGeralVendas += valor;
                totalPedidosGeral++;

                totaisPorCliente.put(nomeCliente, totaisPorCliente.getOrDefault(nomeCliente, 0.0) + valor);
                pedidosPorCliente.put(nomeCliente, pedidosPorCliente.getOrDefault(nomeCliente, 0) + 1);
            }
        }

        String etiquetaStatusFiltro = termoFiltro.isEmpty() ? "SEM FILTRO" : "FILTRO: '" + termoFiltro + "'";
        System.out.println(CoresAnsi.titulo("\n--- RESULTADO [" + etiquetaStatusFiltro + "] ---"));

        if (totaisPorCliente.isEmpty()) {
            System.out.println(CoresAnsi.aviso(" Nenhum registro encontrado.\n"));
            System.out.print("Pressione ENTER para retornar: ");
            lerLinhaSegura(leitorEntrada);
            return;
        }

        System.out.printf("%-24s | %-8s | %-15s | %-15s\n", "CLIENTE", "PEDIDOS", "VOLUME TOTAL", "TICKET MÉDIO");
        System.out.println("----------------------------------------------------------------------------------");
        for (String cliente : totaisPorCliente.keySet()) {
            double vol = totaisPorCliente.get(cliente);
            int qtdPed = pedidosPorCliente.get(cliente);
            double ticketMedioCliente = vol / qtdPed;
            String volExibido = sessao.exibirValorCondicional(Autoridade.FIN_VER_VALORES_SENSIVEIS, vol);
            String ticketExibido = sessao.exibirValorCondicional(Autoridade.FIN_VER_VALORES_SENSIVEIS, ticketMedioCliente);
            System.out.printf("%-24s | %8d | %15s | %15s\n",
                    cliente, qtdPed, volExibido, ticketExibido);
        }
        System.out.println("----------------------------------------------------------------------------------");
        double ticketMedioGeral = (totalPedidosGeral > 0) ? (totalGeralVendas / totalPedidosGeral) : 0.0;
        System.out.printf("TOTAIS: %d pedidos | Volume: %s | Ticket Médio Geral: %s\n",
                totalPedidosGeral,
                sessao.exibirValorCondicional(Autoridade.FIN_VER_VALORES_SENSIVEIS, totalGeralVendas),
                sessao.exibirValorCondicional(Autoridade.FIN_VER_VALORES_SENSIVEIS, ticketMedioGeral));
        System.out.println("==================================================================================\n");

        System.out.print("Deseja exportar para arquivo .TXT? [S/N]: ");
        if (lerLinhaSegura(leitorEntrada).toUpperCase().equals("S")) {
            String nomeArquivoRelatorio = "relatorio_volume_compras_clientes.txt";
            try (BufferedWriter escritorArquivo = new BufferedWriter(new FileWriter(nomeArquivoRelatorio))) {
                escritorArquivo.write("==================================================================================\n");
                escritorArquivo.write("               RELATÓRIO DE COMPRAS POR CLIENTE                 \n");
                escritorArquivo.write("==================================================================================\n");
                escritorArquivo.write("Filtro Aplicado: " + (termoFiltro.isEmpty() ? "NENHUM" : termoFiltro) + "\n");
                escritorArquivo.write("----------------------------------------------------------------------------------\n");
                escritorArquivo.write(String.format("%-24s | %-8s | %-15s | %-15s\n", "CLIENTE", "PEDIDOS", "VOLUME TOTAL", "TICKET MÉDIO"));
                escritorArquivo.write("----------------------------------------------------------------------------------\n");
                for (String cliente : totaisPorCliente.keySet()) {
                    double vol = totaisPorCliente.get(cliente);
                    int qtdPed = pedidosPorCliente.get(cliente);
                    double ticketMedioCliente = vol / qtdPed;
                    escritorArquivo.write(String.format("%-24s | %8d | %15s | %15s\n",
                            cliente, qtdPed, formatarValorMonetarioReais(vol), formatarValorMonetarioReais(ticketMedioCliente)));
                }
                escritorArquivo.write("----------------------------------------------------------------------------------\n");
                escritorArquivo.write(String.format("TOTAIS: %d pedidos | Volume: %s | Ticket Médio Geral: %s\n",
                        totalPedidosGeral, formatarValorMonetarioReais(totalGeralVendas), formatarValorMonetarioReais(ticketMedioGeral)));
                escritorArquivo.write("==================================================================================\n");

                caixa.registrarMovimentoDados("SAIDA_DADOS", "Operador: " + sessao.obterNomeVigente(), "Exportação de relatório para '" + nomeArquivoRelatorio + "'");
                auditoria.registrar(sessao.obterNomeVigente(), "Exportou relatório para " + nomeArquivoRelatorio);
                System.out.println(CoresAnsi.sucesso(" Relatório exportado para '" + nomeArquivoRelatorio + "'.\n"));
            } catch (IOException e) {
                System.out.println(CoresAnsi.erro("Erro ao gravar arquivo.\n"));
            }
        }

        System.out.print("Pressione ENTER para retornar: ");
        lerLinhaSegura(leitorEntrada);
    }

    // Exibe histórico paginado de lançamentos financeiros com filtro e paginação
    private static void gerenciarHistoricoTransacoesPaginado(
            Scanner leitorEntrada, ModuloSessao sessao,
            List<String> tabelaTransacaoTipo, List<String> tabelaTransacaoEnvolvidos,
            List<String> tabelaTransacaoDescricao, List<Double> tabelaTransacaoValor) {

        int paginaAtual = 1;
        final int ITENS_POR_PAGINA = 25;
        String filtro = "";
        boolean navegando = true;

        while (navegando) {
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < tabelaTransacaoTipo.size(); i++) {
                String linhaBusca = (tabelaTransacaoTipo.get(i) + " " + tabelaTransacaoEnvolvidos.get(i) + " " + tabelaTransacaoDescricao.get(i)).toUpperCase();
                if (filtro.isEmpty() || linhaBusca.contains(filtro)) indices.add(i);
            }

            int total = indices.size();
            int totalPaginas = Math.max(1, (int) Math.ceil((double) total / ITENS_POR_PAGINA));
            if (paginaAtual > totalPaginas) paginaAtual = totalPaginas;

            String etiquetaFiltro = filtro.isEmpty() ? "SEM FILTRO" : "FILTRO: '" + filtro + "'";
            System.out.println(CoresAnsi.titulo("\n--- HISTÓRICO DE LANÇAMENTOS [" + etiquetaFiltro + "] (Página " + paginaAtual + " de " + totalPaginas + " | Encontrados: " + total + ") ---"));

            if (total == 0) {
                System.out.println(CoresAnsi.aviso(" Nenhum lançamento encontrado."));
            } else {
                int ini = (paginaAtual - 1) * ITENS_POR_PAGINA;
                int fim = Math.min(ini + ITENS_POR_PAGINA, total);
                for (int i = ini; i < fim; i++) {
                    int idxReal = indices.get(i);
                    String valorExibido = sessao.exibirValorCondicional(Autoridade.FIN_VER_VALORES_SENSIVEIS, tabelaTransacaoValor.get(idxReal));
                    System.out.printf("[%d] [%s] %s | %s | Valor: %s\n",
                            (i + 1), tabelaTransacaoTipo.get(idxReal), tabelaTransacaoEnvolvidos.get(idxReal),
                            tabelaTransacaoDescricao.get(idxReal), valorExibido);
                }
            }

            System.out.println("----------------------------------------------------------------");
            System.out.println("[N] Próxima | [P] Anterior | [F] Filtrar | [L] Limpar Filtro | [0] Voltar");
            System.out.print("Comando: ");
            String cmd = lerLinhaSegura(leitorEntrada).toUpperCase();

            if (cmd.equals("N") && paginaAtual < totalPaginas) paginaAtual++;
            else if (cmd.equals("P") && paginaAtual > 1) paginaAtual--;
            else if (cmd.equals("F")) {
                String termo = validarELerTextoLivreObrigatorio(leitorEntrada, "Termo para filtrar [0 para cancelar]: ");
                if (!termo.equals("CANCELAR")) {
                    filtro = termo;
                    paginaAtual = 1;
                }
            } else if (cmd.equals("L")) {
                filtro = "";
                paginaAtual = 1;
                System.out.println(CoresAnsi.sucesso(" Filtro removido.\n"));
            } else if (isComandoCancelamento(cmd)) navegando = false;
            else System.out.println(CoresAnsi.erro("Comando inválido.\n"));
        }
    }

    // Auxiliar de cadastro de novo item no catálogo (chamado diretamente da tabela unificada de catálogo)
    private static void cadastrarItemCatalogoNaTabela(
            Scanner leitorEntrada,
            ModuloSessao sessao,
            ModuloAuditoria auditoria,
            ModuloCaixa caixa,
            ModuloMovimentacaoEstoque movEstoque,
            List<String> tabelaProdNome, List<String> tabelaProdNcm, List<Double> tabelaProdPrecoVenda, List<Integer> tabelaProdQtdEstoque) {

        if (!sessao.exigirAutoridade(Autoridade.EST_GERIR_CATALOGO, "Sem autoridade para cadastrar item.")) return;

        String nomeProduto = validarELerTextoLivreObrigatorio(leitorEntrada, "Nome do item [0 para cancelar]: ");
        if (nomeProduto.equals("CANCELAR")) return;
        if (tabelaProdNome.contains(nomeProduto)) {
            System.out.println(CoresAnsi.erro("[Duplicidade]: Já existe um item com o nome '" + nomeProduto + "'.\n"));
            return;
        }

        double precoVenda = validarELerValorDecimalMonetario(leitorEntrada, "Preço unitário (R$) [0 para cancelar]: ", false);
        if (precoVenda <= 0) return;

        int estoqueInicial = validarELerInteiroNaoNegativoComSair(leitorEntrada, "Quantidade inicial [SAIR para cancelar]: ");
        if (estoqueInicial == -999) return;

        tabelaProdNome.add(nomeProduto);
        tabelaProdNcm.add("2106.90.30");
        tabelaProdPrecoVenda.add(precoVenda);
        tabelaProdQtdEstoque.add(estoqueInicial);

        if (movEstoque != null) {
            movEstoque.registrarMovimento("CADASTRO_ITEM", nomeProduto, estoqueInicial, estoqueInicial, "Catálogo", sessao.obterNomeVigente());
        }

        caixa.registrarMovimentoDados("ENTRADA_DADOS", "Operador: " + sessao.obterNomeVigente(), "Cadastro de item '" + nomeProduto + "' (" + estoqueInicial + " un @ " + formatarValorMonetarioReais(precoVenda) + ")");
        auditoria.registrar(sessao.obterNomeVigente(), "Cadastrou item: " + nomeProduto);
        System.out.println(CoresAnsi.sucesso(" Item adicionado.\n"));
    }

    // Submenu renumerado e agrupado por domínio: Estoque [1-2], Financeiro [3-5], Logs/Backup [6-9], Voltar [0]
    public static void exibirMenuFinanceiroEstoqueEHistorico(
            Scanner leitorEntrada,
            ModuloSessao sessao,
            ModuloAuditoria auditoria,
            ModuloCaixa caixa,
            ModuloLogDadosTxt logDados,
            ModuloPersistenciaTxt persistencia,
            ModuloAutoridades moduloAutoridades,
            ModuloMovimentacaoEstoque movEstoque,
            List<String> tabelaProdNome, List<String> tabelaProdNcm, List<Double> tabelaProdPrecoVenda, List<Integer> tabelaProdQtdEstoque,
            List<String> tabelaClienteCpf, List<String> tabelaClienteNome,
            List<Double> indicadorGastosEstoque, List<Double> indicadorReceitaBrutaVendas,
            List<Double> indicadorCustoMercadoriaVendida, List<Double> indicadorTotalSalariosPagos,
            List<Double> indicadorTotalFornecedoresPagos, List<Double> indicadorImpostosProvisionados,
            List<String> tabelaTransacaoTipo, List<String> tabelaTransacaoEnvolvidos, List<String> tabelaTransacaoDescricao, List<Double> tabelaTransacaoValor,
            List<String> tabelaFuncId, List<String> tabelaFuncNome, List<String> tabelaFuncCpf, List<String> tabelaFuncCargo,
            List<String> tabelaFuncSenha, List<Double> tabelaFuncSalario, List<Boolean> tabelaFuncPago, List<String> tabelaFuncStatus, List<String> tabelaFuncMotivoInativo,
            List<String> tabelaFornRazaoSocial, List<String> tabelaFornCnpj, List<String> tabelaFornUf,
            List<String> tabelaFornProdutoPrincipal, List<Double> tabelaFornValorFatura,
            List<Double> tabelaFornMargemLucroPct, List<Boolean> tabelaFornFaturaPaga) {

        boolean menuFinanceiroAtivo = true;
        while (menuFinanceiroAtivo) {
            if (!sessao.validarOuRevogarSessao()) return;

            // Exibe o menu agrupado por Estoque, Financeiro, Logs/Persistência e Retorno ([0])
            System.out.println(CoresAnsi.titulo("\n================================================================"));
            System.out.println(CoresAnsi.titulo("                     OPERAÇÕES E REGISTROS                      "));
            System.out.println(CoresAnsi.titulo("================================================================"));
            if (sessao.temAutoridade(Autoridade.EST_VER_CATALOGO)) {
                System.out.println("[1] Catálogo, saldos e itens (25/pág)");
                System.out.println("[2] Movimentações de estoque (25/pág)");
            }
            if (sessao.temAutoridade(Autoridade.FIN_VER_VALORES_SENSIVEIS)) {
                System.out.println("[3] Histórico de lançamentos (25/pág)");
                System.out.println("[4] Relatório por cliente (.TXT)");
                System.out.println("[5] Consolidado financeiro");
            }
            if (sessao.temAutoridade(Autoridade.LOG_VER_E_EXPORTAR)) {
                System.out.println("[6] Log de entradas e saídas (.TXT)");
            }
            if (sessao.temAutoridade(Autoridade.SIS_AUDITORIA_E_BACKUP)) {
                System.out.println("[7] Registros de auditoria (25/pág)");
                System.out.println("[8] Salvar backup (.TXT)");
                System.out.println("[9] Carregar backup (.TXT)");
            }
            System.out.println("[0] Voltar");
            System.out.println(CoresAnsi.titulo("================================================================"));

            // Roteia as opções renumeradas validando a autoridade correspondente de cada ação
            int opcaoEscolhida = validarELerNumeroInteiro(leitorEntrada, "Selecione: ");
            if (opcaoEscolhida == 0 || opcaoEscolhida == -999) {
                menuFinanceiroAtivo = false;
            } else if (opcaoEscolhida == 1) {
                if (!sessao.exigirAutoridade(Autoridade.EST_VER_CATALOGO, "Sem autoridade para ver o catálogo.")) continue;
                garantirPreCadastrosAtualizadosDoTxt(
                        persistencia, caixa, moduloAutoridades,
                        tabelaFuncId, tabelaFuncNome, tabelaFuncCpf,
                        tabelaFuncCargo, tabelaFuncSenha, tabelaFuncSalario,
                        tabelaFuncPago, tabelaFuncStatus, tabelaFuncMotivoInativo,
                        tabelaFornRazaoSocial, tabelaFornCnpj, tabelaFornUf,
                        tabelaFornProdutoPrincipal, tabelaFornValorFatura,
                        tabelaFornMargemLucroPct, tabelaFornFaturaPaga,
                        tabelaProdNome, tabelaProdNcm, tabelaProdPrecoVenda,
                        tabelaProdQtdEstoque, tabelaClienteCpf, tabelaClienteNome
                );
                gerenciarCatalogoProdutosPaginado(
                        leitorEntrada, sessao, auditoria, caixa, movEstoque,
                        tabelaProdNome, tabelaProdNcm, tabelaProdPrecoVenda, tabelaProdQtdEstoque
                );
            } else if (opcaoEscolhida == 2) {
                if (!sessao.exigirAutoridade(Autoridade.EST_VER_CATALOGO, "Sem autoridade para consultar movimentações de estoque.")) continue;
                exibirMovimentacoesEstoquePaginado(leitorEntrada, movEstoque);
            } else if (opcaoEscolhida == 3) {
                if (!sessao.exigirAutoridade(Autoridade.FIN_VER_VALORES_SENSIVEIS, "Sem autoridade para ver lançamentos financeiros.")) continue;
                gerenciarHistoricoTransacoesPaginado(leitorEntrada, sessao, tabelaTransacaoTipo, tabelaTransacaoEnvolvidos, tabelaTransacaoDescricao, tabelaTransacaoValor);
            } else if (opcaoEscolhida == 4) {
                if (!sessao.exigirAutoridade(Autoridade.FIN_VER_VALORES_SENSIVEIS, "Sem autoridade para gerar relatório financeiro.")) continue;
                exibirRelatorioVolumeComprasClientes(leitorEntrada, auditoria, sessao, caixa, tabelaTransacaoTipo, tabelaTransacaoEnvolvidos, tabelaTransacaoDescricao, tabelaTransacaoValor);
            } else if (opcaoEscolhida == 5) {
                if (!sessao.exigirAutoridade(Autoridade.FIN_VER_VALORES_SENSIVEIS, "Sem autoridade para ver o consolidado financeiro.")) continue;
                System.out.println(CoresAnsi.titulo("\n================ DEMONSTRATIVO CONSOLIDADO ================"));
                System.out.println("• Saldo Atual em Caixa Global      : " + CoresAnsi.sucesso(formatarValorMonetarioReais(caixa.obterSaldo())));
                System.out.println("• Receita Bruta de Vendas          : " + formatarValorMonetarioReais(indicadorReceitaBrutaVendas.get(0)));
                System.out.println("• Custo Mercadoria Vendida (CMV)   : " + formatarValorMonetarioReais(indicadorCustoMercadoriaVendida.get(0)));
                System.out.println("• Investimento / Gastos Estoque    : " + formatarValorMonetarioReais(indicadorGastosEstoque.get(0)));
                System.out.println("• Folha de Pagamento Quitada       : " + formatarValorMonetarioReais(indicadorTotalSalariosPagos.get(0)));
                System.out.println("• Fornecedores Quitados            : " + formatarValorMonetarioReais(indicadorTotalFornecedoresPagos.get(0)));
                System.out.println("• Impostos Provisionados (ICMS)    : " + formatarValorMonetarioReais(indicadorImpostosProvisionados.get(0)));
                System.out.println(CoresAnsi.titulo("============================================================\n"));
            } else if (opcaoEscolhida == 6) {
                if (!sessao.exigirAutoridade(Autoridade.LOG_VER_E_EXPORTAR, "Sem autoridade para consultar/exportar log.")) continue;
                exibirLogEntradasSaidasPaginado(leitorEntrada, sessao, auditoria, caixa, logDados);
            } else if (opcaoEscolhida == 7) {
                exibirLogAuditoriaPaginado(leitorEntrada, sessao, auditoria);
            } else if (opcaoEscolhida == 8) {
                persistencia.exportarComConfirmacao(
                        leitorEntrada, sessao, auditoria,
                        tabelaFuncId, tabelaFuncNome, tabelaFuncCpf, tabelaFuncCargo, tabelaFuncSenha,
                        tabelaFuncSalario, tabelaFuncPago, tabelaFuncStatus, tabelaFuncMotivoInativo,
                        tabelaProdNome, tabelaProdNcm, tabelaProdPrecoVenda, tabelaProdQtdEstoque,
                        tabelaTransacaoTipo.size(),
                        caixa
                );
            } else if (opcaoEscolhida == 9) {
                persistencia.restaurarBackupOficial(
                        leitorEntrada, sessao, auditoria, moduloAutoridades,
                        tabelaFuncId, tabelaFuncNome, tabelaFuncCpf, tabelaFuncCargo, tabelaFuncSenha,
                        tabelaFuncSalario, tabelaFuncPago, tabelaFuncStatus, tabelaFuncMotivoInativo,
                        tabelaProdNome, tabelaProdNcm, tabelaProdPrecoVenda, tabelaProdQtdEstoque,
                        caixa
                );
            } else {
                System.out.println(CoresAnsi.erro("Opção indisponível.\n"));
            }
        }
    }

    // Exibe movimentações de estoque paginadas (25 por página) com filtro
    private static void exibirMovimentacoesEstoquePaginado(Scanner leitorEntrada, ModuloMovimentacaoEstoque movEstoque) {
        if (movEstoque == null) {
            System.out.println(CoresAnsi.aviso(" Módulo de movimentações de estoque indisponível.\n"));
            return;
        }

        int paginaAtual = 1;
        final int ITENS_POR_PAGINA = 25;
        String filtroAtivo = "";
        boolean navegando = true;

        while (navegando) {
            List<String> lista = movEstoque.buscarFiltrados(filtroAtivo);
            int total = lista.size();
            int totalPaginas = Math.max(1, (int) Math.ceil((double) total / ITENS_POR_PAGINA));
            if (paginaAtual > totalPaginas) paginaAtual = totalPaginas;

            String etiquetaFiltro = filtroAtivo.isEmpty() ? "SEM FILTRO" : "FILTRO: '" + filtroAtivo + "'";
            System.out.println(CoresAnsi.titulo("\n--- MOVIMENTAÇÕES DE ESTOQUE [" + etiquetaFiltro + "] (Página " + paginaAtual + " de " + totalPaginas + " | Encontrados: " + total + ") ---"));

            if (total == 0) {
                System.out.println(CoresAnsi.aviso(" Nenhuma movimentação encontrada para o filtro '" + filtroAtivo + "'. Use [L] para limpar."));
            } else {
                int ini = (paginaAtual - 1) * ITENS_POR_PAGINA;
                int fim = Math.min(ini + ITENS_POR_PAGINA, total);
                for (int i = ini; i < fim; i++) {
                    System.out.printf("[%d] %s\n", (i + 1), lista.get(i));
                }
            }

            System.out.println("----------------------------------------------------------------");
            System.out.println("[N] Próxima | [P] Anterior | [F] Filtrar | [L] Limpar Filtro | [0] Voltar");
            System.out.print("Comando: ");
            String cmd = lerLinhaSegura(leitorEntrada).toUpperCase();

            if (cmd.equals("N") && paginaAtual < totalPaginas) paginaAtual++;
            else if (cmd.equals("P") && paginaAtual > 1) paginaAtual--;
            else if (cmd.equals("F")) {
                String termo = validarELerTextoLivreObrigatorio(leitorEntrada, "Termo para filtrar [0 para cancelar]: ");
                if (!termo.equals("CANCELAR")) {
                    filtroAtivo = termo;
                    paginaAtual = 1;
                }
            } else if (cmd.equals("L")) {
                filtroAtivo = "";
                paginaAtual = 1;
                System.out.println(CoresAnsi.sucesso(" Filtro removido.\n"));
            } else if (isComandoCancelamento(cmd)) {
                navegando = false;
            } else {
                System.out.println(CoresAnsi.erro("Comando inválido.\n"));
            }
        }
    }

    // Exibe log de entradas/saídas paginado com filtro e exportação para arquivo .TXT personalizado
    private static void exibirLogEntradasSaidasPaginado(
            Scanner leitorEntrada,
            ModuloSessao sessao,
            ModuloAuditoria auditoria,
            ModuloCaixa caixa,
            ModuloLogDadosTxt logDados) {

        if (logDados == null) {
            System.out.println(CoresAnsi.aviso(" Módulo de log indisponível.\n"));
            return;
        }

        int paginaAtual = 1;
        final int ITENS_POR_PAGINA = 25;
        String filtroAtivo = "";
        boolean navegandoLog = true;

        while (navegandoLog) {
            List<String> listaRegistrosFiltrados = logDados.lerEventosFiltrados(filtroAtivo);
            int totalRegistros = listaRegistrosFiltrados.size();
            int totalPaginas = Math.max(1, (int) Math.ceil((double) totalRegistros / ITENS_POR_PAGINA));
            if (paginaAtual > totalPaginas) paginaAtual = totalPaginas;

            String etiquetaFiltro = filtroAtivo.isEmpty() ? "SEM FILTRO" : "FILTRO: '" + filtroAtivo + "'";
            System.out.println(CoresAnsi.titulo("\n--- LOG DE ENTRADAS E SAÍDAS DE DADOS [" + etiquetaFiltro + "] (Página " + paginaAtual + " de " + totalPaginas + " | Encontrados: " + totalRegistros + ") ---"));

            if (totalRegistros == 0) {
                System.out.println(CoresAnsi.aviso(" Nenhum registro corresponde ao filtro '" + filtroAtivo + "'. Pressione [L] para limpar."));
            } else {
                int indiceInicial = (paginaAtual - 1) * ITENS_POR_PAGINA;
                int indiceFinal = Math.min(indiceInicial + ITENS_POR_PAGINA, totalRegistros);
                for (int i = indiceInicial; i < indiceFinal; i++) {
                    System.out.printf("[%d] %s\n", (i + 1), listaRegistrosFiltrados.get(i));
                }
            }

            System.out.println("----------------------------------------------------------------");
            System.out.println("[N] Próxima | [P] Anterior | [F] Filtrar | [L] Limpar Filtro | [E] Exportar .TXT | [0] Voltar");
            System.out.print("Comando: ");
            String comandoUsuario = lerLinhaSegura(leitorEntrada).toUpperCase();

            if (comandoUsuario.equals("N") && paginaAtual < totalPaginas) paginaAtual++;
            else if (comandoUsuario.equals("P") && paginaAtual > 1) paginaAtual--;
            else if (comandoUsuario.equals("F")) {
                String termoBusca = validarELerTextoLivreObrigatorio(leitorEntrada, "Termo para filtrar [0 para cancelar]: ");
                if (!termoBusca.equals("CANCELAR")) {
                    filtroAtivo = termoBusca;
                    paginaAtual = 1;
                }
            } else if (comandoUsuario.equals("L")) {
                filtroAtivo = "";
                paginaAtual = 1;
                System.out.println(CoresAnsi.sucesso(" Filtro removido.\n"));
            } else if (comandoUsuario.equals("E")) {
                if (listaRegistrosFiltrados.isEmpty()) {
                    System.out.println(CoresAnsi.aviso(" Não há eventos filtrados para exportar.\n"));
                    continue;
                }
                System.out.print("Nome do arquivo .TXT [ENTER para '" + ARQUIVO_LOG_FILTRADO_EXPORTADO + "' | 0 para cancelar]: ");
                String nomeDigitado = lerLinhaSegura(leitorEntrada);
                if (isComandoCancelamento(nomeDigitado)) {
                    System.out.println(CoresAnsi.aviso(" Exportação cancelada.\n"));
                    continue;
                }
                // Sanitiza caracteres inválidos de caminho e garante extensão .txt
                String nomeBase = nomeDigitado.trim().isEmpty()
                        ? ARQUIVO_LOG_FILTRADO_EXPORTADO
                        : nomeDigitado.trim().replaceAll("[\\\\/:*?\"<>|]", "_").replaceAll("\\s+", "_");
                if (!nomeBase.toLowerCase().endsWith(".txt")) {
                    nomeBase = nomeBase + ".txt";
                }
                final String arquivoDestino = nomeBase;

                // Pede confirmação caso o arquivo de destino já exista
                File arquivoExistente = new File(arquivoDestino);
                if (arquivoExistente.exists()) {
                    System.out.print(CoresAnsi.aviso(" O arquivo '" + arquivoDestino + "' já existe. Deseja sobrescrever? [S/N]: "));
                    String respostaSobrescrita = lerLinhaSegura(leitorEntrada).toUpperCase();
                    if (!respostaSobrescrita.equals("S")) {
                        System.out.println(CoresAnsi.aviso(" Sobrescrita cancelada. O arquivo existente foi mantido.\n"));
                        continue;
                    }
                }

                try (BufferedWriter escritor = new BufferedWriter(new FileWriter(arquivoDestino))) {
                    escritor.write("================================================================================\n");
                    escritor.write("# LOG DE ENTRADAS E SAÍDAS EXPORTADO - PROTEI CREAT (L'ESTOQUE)\n");
                    escritor.write("# Gerado em: " + LocalDateTime.now().format(FORMATADOR_DATA_HORA) + "\n");
                    escritor.write("# Filtro aplicado: " + (filtroAtivo.isEmpty() ? "NENHUM" : filtroAtivo) + "\n");
                    escritor.write("# Total de eventos: " + totalRegistros + "\n");
                    escritor.write("================================================================================\n");
                    for (String evento : listaRegistrosFiltrados) {
                        escritor.write(evento + "\n");
                    }
                    String autor = (sessao != null) ? sessao.obterNomeVigente() : "SISTEMA";
                    if (caixa != null) {
                        caixa.registrarMovimentoDados(
                                "SAIDA_DADOS",
                                "Operador: " + autor,
                                "Exportação de " + totalRegistros + " evento(s) do log para '" + arquivoDestino + "' (" + etiquetaFiltro + ")"
                        );
                    }
                    if (auditoria != null) {
                        auditoria.registrar(autor, "Exportou eventos filtrados do log para " + arquivoDestino);
                    }
                    System.out.println(CoresAnsi.sucesso(" " + totalRegistros + " evento(s) exportado(s) para '" + arquivoDestino + "'.\n"));
                } catch (IOException e) {
                    System.out.println(CoresAnsi.erro("Erro ao exportar log para '" + arquivoDestino + "': " + e.getMessage() + "\n"));
                }
            } else if (isComandoCancelamento(comandoUsuario)) {
                navegandoLog = false;
            } else {
                System.out.println(CoresAnsi.erro("Comando inválido.\n"));
            }
        }
    }

    // Exibe os registros de auditoria em memória de forma paginada e filtrável
    private static void exibirLogAuditoriaPaginado(Scanner leitorEntrada, ModuloSessao sessao, ModuloAuditoria auditoria) {
        if (!sessao.exigirAutoridade(Autoridade.SIS_AUDITORIA_E_BACKUP, "Sem autoridade para visualizar a auditoria.")) return;
        if (auditoria.estaVazio()) {
            System.out.println(CoresAnsi.aviso(" Nenhum registro encontrado.\n"));
            return;
        }

        int paginaAtual = 1;
        final int ITENS_POR_PAGINA = 25;
        String filtroAtivo = "";
        boolean navegandoLog = true;

        while (navegandoLog) {
            List<String> listaRegistrosFiltrados = auditoria.buscarFiltrados(filtroAtivo);
            int totalRegistros = listaRegistrosFiltrados.size();
            int totalPaginas = Math.max(1, (int) Math.ceil((double) totalRegistros / ITENS_POR_PAGINA));
            if (paginaAtual > totalPaginas) paginaAtual = totalPaginas;

            String etiquetaFiltro = filtroAtivo.isEmpty() ? "SEM FILTRO" : "FILTRO: '" + filtroAtivo + "'";
            System.out.println(CoresAnsi.titulo("\n--- REGISTROS DE AUDITORIA [" + etiquetaFiltro + "] (Página " + paginaAtual + " de " + totalPaginas + " | Encontrados: " + totalRegistros + ") ---"));

            if (totalRegistros == 0) {
                System.out.println(CoresAnsi.aviso(" Nenhum registro corresponde ao filtro '" + filtroAtivo + "'. Pressione [L] para limpar."));
            } else {
                int indiceInicial = (paginaAtual - 1) * ITENS_POR_PAGINA;
                int indiceFinal = Math.min(indiceInicial + ITENS_POR_PAGINA, totalRegistros);
                for (int i = indiceInicial; i < indiceFinal; i++) {
                    System.out.printf("[%d] %s\n", (i + 1), listaRegistrosFiltrados.get(i));
                }
            }

            System.out.println("----------------------------------------------------------------");
            System.out.println("[N] Próxima | [P] Anterior | [F] Filtrar | [L] Limpar Filtro | [0] Voltar");
            System.out.print("Comando: ");
            String comandoUsuario = lerLinhaSegura(leitorEntrada).toUpperCase();

            if (comandoUsuario.equals("N") && paginaAtual < totalPaginas) paginaAtual++;
            else if (comandoUsuario.equals("P") && paginaAtual > 1) paginaAtual--;
            else if (comandoUsuario.equals("F")) {
                String termo = validarELerTextoLivreObrigatorio(leitorEntrada, "Termo para filtrar [0 para cancelar]: ");
                if (!termo.equals("CANCELAR")) {
                    filtroAtivo = termo;
                    paginaAtual = 1;
                }
            } else if (comandoUsuario.equals("L")) {
                filtroAtivo = "";
                paginaAtual = 1;
                System.out.println(CoresAnsi.sucesso(" Filtro removido.\n"));
            } else if (isComandoCancelamento(comandoUsuario)) {
                navegandoLog = false;
            } else {
                System.out.println(CoresAnsi.erro("Comando inválido.\n"));
            }
        }
    }

    // Tabela paginada unificada do catálogo de produtos com atalho [C] para cadastrar item sem menu separado
    private static void gerenciarCatalogoProdutosPaginado(
            Scanner leitorEntrada,
            ModuloSessao sessao,
            ModuloAuditoria auditoria,
            ModuloCaixa caixa,
            ModuloMovimentacaoEstoque movEstoque,
            List<String> tabelaProdNome, List<String> tabelaProdNcm, List<Double> tabelaProdPrecoVenda, List<Integer> tabelaProdQtdEstoque) {

        int paginaAtual = 1;
        final int ITENS_POR_PAGINA = 25;
        String filtroCatalogo = "";
        boolean navegandoCatalogo = true;

        while (navegandoCatalogo) {
            List<Integer> indicesFiltrados = new ArrayList<>();
            for (int i = 0; i < tabelaProdNome.size(); i++) {
                String busca = (tabelaProdNome.get(i) + " " + tabelaProdNcm.get(i)).toUpperCase();
                if (filtroCatalogo.isEmpty() || busca.contains(filtroCatalogo)) {
                    indicesFiltrados.add(i);
                }
            }

            int totalFiltrados = indicesFiltrados.size();
            int totalPaginas = Math.max(1, (int) Math.ceil((double) totalFiltrados / ITENS_POR_PAGINA));
            if (paginaAtual > totalPaginas) paginaAtual = totalPaginas;

            String etiquetaFiltro = filtroCatalogo.isEmpty() ? "TODOS" : "FILTRO: '" + filtroCatalogo + "'";
            System.out.println(CoresAnsi.titulo("\n--- CATÁLOGO E SALDOS [" + etiquetaFiltro + "] (Página " + paginaAtual + " de " + totalPaginas + " | Itens: " + totalFiltrados + ") ---"));

            if (tabelaProdNome.isEmpty()) {
                System.out.println(CoresAnsi.aviso(" Nenhum item cadastrado no catálogo."));
            } else if (totalFiltrados == 0) {
                System.out.println(CoresAnsi.aviso(" Nenhum item corresponde ao filtro '" + filtroCatalogo + "'. Use [L] para limpar."));
            } else {
                int indiceInicial = (paginaAtual - 1) * ITENS_POR_PAGINA;
                int indiceFinal = Math.min(indiceInicial + ITENS_POR_PAGINA, totalFiltrados);
                for (int i = indiceInicial; i < indiceFinal; i++) {
                    int idxReal = indicesFiltrados.get(i);
                    int saldo = tabelaProdQtdEstoque.get(idxReal);
                    System.out.printf("[%d] %s | NCM: %s | Preço: %s | Estoque: %d un%s\n",
                            (i + 1), tabelaProdNome.get(idxReal), tabelaProdNcm.get(idxReal),
                            formatarValorMonetarioReais(tabelaProdPrecoVenda.get(idxReal)),
                            saldo, formatarAlertaEstoque(saldo));
                }
            }

            // Barra de comandos unificada do Catálogo: evita alternar entre tela de consulta e tela de novo cadastro
            System.out.println("----------------------------------------------------------------");
            System.out.print("[N] Próxima | [P] Anterior | [F] Filtrar | [L] Limpar");
            if (sessao.temAutoridade(Autoridade.EST_GERIR_CATALOGO)) {
                System.out.print(" | [C] Cadastrar Item");
            }
            System.out.println(" | [0] Voltar");
            System.out.print("Comando: ");
            String comandoCatalogo = lerLinhaSegura(leitorEntrada).toUpperCase();

            if (comandoCatalogo.equals("N") && paginaAtual < totalPaginas) paginaAtual++;
            else if (comandoCatalogo.equals("P") && paginaAtual > 1) paginaAtual--;
            else if (comandoCatalogo.equals("F")) {
                String termo = validarELerTextoLivreObrigatorio(leitorEntrada, "Filtrar item [0 para cancelar]: ");
                if (!termo.equals("CANCELAR")) {
                    filtroCatalogo = termo;
                    paginaAtual = 1;
                }
            } else if (comandoCatalogo.equals("L")) {
                filtroCatalogo = "";
                paginaAtual = 1;
                System.out.println(CoresAnsi.sucesso(" Filtro do catálogo removido.\n"));
            } else if (comandoCatalogo.equals("C")) {
                cadastrarItemCatalogoNaTabela(
                        leitorEntrada, sessao, auditoria, caixa, movEstoque,
                        tabelaProdNome, tabelaProdNcm, tabelaProdPrecoVenda, tabelaProdQtdEstoque
                );
            } else if (isComandoCancelamento(comandoCatalogo)) {
                navegandoCatalogo = false;
            } else {
                System.out.println(CoresAnsi.erro("Comando inválido.\n"));
            }
        }
    }

    // =========================================================================
    // MÓDULO 10: PORTAL DO CLIENTE E CARRINHO DE COMPRAS
    // Isola a vitrine paginada, reserva de itens no carrinho, consulta de
    // histórico por CPF e fechamento de venda sem depender de estado administrativo.
    // =========================================================================

    // Soma a quantidade de um produto já reservada no carrinho atual
    private static int obterQuantidadeReservadaNoCarrinho(int indiceProduto, List<Integer> carrinhoIndices, List<Integer> carrinhoQuantidades) {
        int totalReservado = 0;
        for (int i = 0; i < carrinhoIndices.size(); i++) {
            if (carrinhoIndices.get(i) == indiceProduto) {
                totalReservado += carrinhoQuantidades.get(i);
            }
        }
        return totalReservado;
    }

    // Exibe compras anteriores filtradas por CPF informado
    private static void consultarHistoricoClientePorCpf(
            Scanner leitorEntrada, List<String> tabelaClienteCpf, List<String> tabelaClienteNome,
            List<String> tabelaTransacaoTipo, List<String> tabelaTransacaoEnvolvidos,
            List<String> tabelaTransacaoDescricao, List<Double> tabelaTransacaoValor) {

        String cpfBusca = validarEFormatarCpf(leitorEntrada);
        if (cpfBusca.equals("CANCELAR")) return;

        int idxCliente = tabelaClienteCpf.indexOf(cpfBusca);
        String nomeCliente = (idxCliente != -1) ? tabelaClienteNome.get(idxCliente) : "CLIENTE";
        System.out.println(CoresAnsi.titulo("\n--- COMPRAS REGISTRADAS PARA CPF " + cpfBusca + " (" + nomeCliente + ") ---"));

        boolean achou = false;
        for (int i = 0; i < tabelaTransacaoTipo.size(); i++) {
            if ("VENDA".equalsIgnoreCase(tabelaTransacaoTipo.get(i)) && tabelaTransacaoEnvolvidos.get(i).contains(cpfBusca)) {
                achou = true;
                System.out.printf("• %s | %s | Valor: %s\n",
                        tabelaTransacaoEnvolvidos.get(i), tabelaTransacaoDescricao.get(i),
                        formatarValorMonetarioReais(tabelaTransacaoValor.get(i)));
            }
        }
        if (!achou) {
            System.out.println(CoresAnsi.aviso(" Nenhuma compra localizada para este CPF.\n"));
        } else {
            System.out.println();
        }
    }

    // Finaliza a compra dos itens reservados no carrinho, atualizando estoque, caixa e indicadores
    private static void gerenciarCarrinhoEFinalizarCompra(
            Scanner leitorEntrada, ModuloCaixa caixa, ModuloMovimentacaoEstoque movEstoque,
            List<Integer> carrinhoIndicesProdutos, List<Integer> carrinhoQuantidades,
            List<String> tabelaProdNome, List<String> tabelaProdNcm, List<Double> tabelaProdPrecoVenda, List<Integer> tabelaProdQtdEstoque,
            List<Double> indicadorReceitaBrutaVendas, List<Double> indicadorCustoMercadoriaVendida, List<Double> indicadorImpostosProvisionados,
            List<String> tabelaClienteCpf, List<String> tabelaClienteNome, int anoExercicioFiscal) {

        if (carrinhoIndicesProdutos.isEmpty()) {
            System.out.println(CoresAnsi.aviso(" Seu carrinho está vazio.\n"));
            return;
        }

        double totalCompra = 0.0;
        System.out.println(CoresAnsi.titulo("\n--- RESUMO DO CARRINHO ---"));
        for (int i = 0; i < carrinhoIndicesProdutos.size(); i++) {
            int idxProd = carrinhoIndicesProdutos.get(i);
            int qtd = carrinhoQuantidades.get(i);
            double subtotal = tabelaProdPrecoVenda.get(idxProd) * qtd;
            totalCompra += subtotal;
            System.out.printf("• %dx %s (%s cada) = %s\n",
                    qtd, tabelaProdNome.get(idxProd),
                    formatarValorMonetarioReais(tabelaProdPrecoVenda.get(idxProd)),
                    formatarValorMonetarioReais(subtotal));
        }
        System.out.println("Total do pedido: " + CoresAnsi.sucesso(formatarValorMonetarioReais(totalCompra)));
        System.out.print("Confirmar compra? [S/N]: ");
        if (!lerLinhaSegura(leitorEntrada).toUpperCase().equals("S")) {
            System.out.println(CoresAnsi.aviso(" Compra mantida no carrinho.\n"));
            return;
        }

        String nomeCliente = validarELerTextoAlfabetico(leitorEntrada, "Nome do cliente: ");
        if (nomeCliente.equals("CANCELAR")) return;
        String cpfCliente = validarEFormatarCpf(leitorEntrada);
        if (cpfCliente.equals("CANCELAR")) return;

        if (!tabelaClienteCpf.contains(cpfCliente)) {
            tabelaClienteCpf.add(cpfCliente);
            tabelaClienteNome.add(nomeCliente);
        }

        double impostosVenda = 0.0;
        StringBuilder resumoItens = new StringBuilder();
        for (int i = 0; i < carrinhoIndicesProdutos.size(); i++) {
            int idxProd = carrinhoIndicesProdutos.get(i);
            int qtd = carrinhoQuantidades.get(i);
            int novoSaldo = Math.max(0, tabelaProdQtdEstoque.get(idxProd) - qtd);
            tabelaProdQtdEstoque.set(idxProd, novoSaldo);
            double subtotal = tabelaProdPrecoVenda.get(idxProd) * qtd;
            impostosVenda += subtotal * calcularAliquotaIcmsPorOperacao("SP", "SP", anoExercicioFiscal);
            if (i > 0) resumoItens.append(", ");
            resumoItens.append(qtd).append("x ").append(tabelaProdNome.get(idxProd));
            if (movEstoque != null) {
                movEstoque.registrarMovimento("SAIDA_VENDA", tabelaProdNome.get(idxProd), -qtd, novoSaldo, "Cliente: " + nomeCliente, "PORTAL_CLIENTE");
            }
        }

        caixa.registrarEntrada(totalCompra, "VENDA", "Cliente: " + nomeCliente + " (CPF: " + cpfCliente + ") | Caixa: PORTAL", resumoItens.toString());
        indicadorReceitaBrutaVendas.set(0, indicadorReceitaBrutaVendas.get(0) + totalCompra);
        indicadorCustoMercadoriaVendida.set(0, indicadorCustoMercadoriaVendida.get(0) + (totalCompra * 0.45));
        indicadorImpostosProvisionados.set(0, indicadorImpostosProvisionados.get(0) + impostosVenda);

        carrinhoIndicesProdutos.clear();
        carrinhoQuantidades.clear();
        System.out.println(CoresAnsi.sucesso(" Compra finalizada com sucesso!\n"));
    }

    // Portal de compras paginado e filtrável do cliente com carrinho em memória
    public static void executarPortalComprasCliente(
            Scanner leitorEntrada, ModuloCaixa caixa, ModuloMovimentacaoEstoque movEstoque,
            List<String> tabelaProdNome, List<String> tabelaProdNcm, List<Double> tabelaProdPrecoVenda, List<Integer> tabelaProdQtdEstoque,
            List<Double> indicadorReceitaBrutaVendas, List<Double> indicadorCustoMercadoriaVendida, List<Double> indicadorImpostosProvisionados,
            List<String> tabelaClienteCpf, List<String> tabelaClienteNome,
            List<String> tabelaTransacaoTipo, List<String> tabelaTransacaoEnvolvidos,
            List<String> tabelaTransacaoDescricao, List<Double> tabelaTransacaoValor,
            int anoExercicioFiscal) {

        List<Integer> carrinhoIndicesProdutos = new ArrayList<>();
        List<Integer> carrinhoQuantidades = new ArrayList<>();
        int paginaAtual = 1;
        final int ITENS_POR_PAGINA = 25;
        String filtroVitrineAtivo = "";
        boolean portalClienteAtivo = true;

        while (portalClienteAtivo) {
            List<Integer> listaIndicesReaisFiltrados = new ArrayList<>();
            for (int i = 0; i < tabelaProdNome.size(); i++) {
                String busca = (tabelaProdNome.get(i) + " " + tabelaProdNcm.get(i)).toUpperCase();
                if (filtroVitrineAtivo.isEmpty() || busca.contains(filtroVitrineAtivo)) {
                    listaIndicesReaisFiltrados.add(i);
                }
            }

            int totalFiltrados = listaIndicesReaisFiltrados.size();
            int totalPaginas = Math.max(1, (int) Math.ceil((double) totalFiltrados / ITENS_POR_PAGINA));
            if (paginaAtual > totalPaginas) paginaAtual = totalPaginas;

            String etiquetaFiltro = filtroVitrineAtivo.isEmpty() ? "TODOS" : "FILTRO: '" + filtroVitrineAtivo + "'";
            System.out.println(CoresAnsi.titulo("\n================================================================"));
            System.out.println(CoresAnsi.titulo("                       PORTAL DO CLIENTE                        "));
            System.out.println(CoresAnsi.titulo("================================================================"));
            System.out.println(CoresAnsi.titulo("--- VITRINE [" + etiquetaFiltro + "] (Página " + paginaAtual + " de " + totalPaginas + ") ---"));

            if (totalFiltrados == 0) {
                System.out.println(CoresAnsi.aviso(" Nenhum produto disponível para exibição."));
            } else {
                int indiceInicial = (paginaAtual - 1) * ITENS_POR_PAGINA;
                int indiceFinal = Math.min(indiceInicial + ITENS_POR_PAGINA, totalFiltrados);
                for (int i = indiceInicial; i < indiceFinal; i++) {
                    int indiceReal = listaIndicesReaisFiltrados.get(i);
                    int reservado = obterQuantidadeReservadaNoCarrinho(indiceReal, carrinhoIndicesProdutos, carrinhoQuantidades);
                    int disponivelParaAdicionar = Math.max(0, tabelaProdQtdEstoque.get(indiceReal) - reservado);
                    String infoCarrinho = reservado > 0 ? CoresAnsi.colorir(CoresAnsi.CIANO, " [No carrinho: " + reservado + "]") : "";

                    System.out.printf("[%d] %s | Preço: %s | Disponível: %d un%s%s\n",
                            (i + 1), tabelaProdNome.get(indiceReal),
                            formatarValorMonetarioReais(tabelaProdPrecoVenda.get(indiceReal)),
                            disponivelParaAdicionar, infoCarrinho, formatarAlertaEstoque(disponivelParaAdicionar));
                }
            }

            System.out.println("----------------------------------------------------------------");
            System.out.println("[N] Próxima | [P] Anterior | [F] Filtrar | [L] Limpar Filtro | [C] Carrinho | [H] Minhas Compras | [NÚMERO] Adicionar | [0] Voltar");
            System.out.print("Escolha: ");
            String comandoCliente = lerLinhaSegura(leitorEntrada).toUpperCase();

            if (comandoCliente.equals("N") && paginaAtual < totalPaginas) paginaAtual++;
            else if (comandoCliente.equals("P") && paginaAtual > 1) paginaAtual--;
            else if (comandoCliente.equals("F")) {
                String termoBusca = validarELerTextoLivreObrigatorio(leitorEntrada, "Buscar item [0 para cancelar]: ");
                if (!termoBusca.equals("CANCELAR")) {
                    filtroVitrineAtivo = termoBusca;
                    paginaAtual = 1;
                }
            } else if (comandoCliente.equals("L")) {
                filtroVitrineAtivo = "";
                paginaAtual = 1;
                System.out.println(CoresAnsi.sucesso(" Filtro removido.\n"));
            } else if (comandoCliente.equals("C")) {
                gerenciarCarrinhoEFinalizarCompra(
                        leitorEntrada, caixa, movEstoque, carrinhoIndicesProdutos, carrinhoQuantidades,
                        tabelaProdNome, tabelaProdNcm, tabelaProdPrecoVenda, tabelaProdQtdEstoque,
                        indicadorReceitaBrutaVendas, indicadorCustoMercadoriaVendida, indicadorImpostosProvisionados,
                        tabelaClienteCpf, tabelaClienteNome, anoExercicioFiscal
                );
            } else if (comandoCliente.equals("H")) {
                consultarHistoricoClientePorCpf(
                        leitorEntrada, tabelaClienteCpf, tabelaClienteNome,
                        tabelaTransacaoTipo, tabelaTransacaoEnvolvidos,
                        tabelaTransacaoDescricao, tabelaTransacaoValor
                );
            } else if (isComandoCancelamento(comandoCliente)) {
                if (!carrinhoIndicesProdutos.isEmpty()) {
                    System.out.print(CoresAnsi.aviso(" Há itens no carrinho. Deseja sair e descartar? [S/N]: "));
                    if (!lerLinhaSegura(leitorEntrada).toUpperCase().equals("S")) continue;
                }
                portalClienteAtivo = false;
            } else if (comandoCliente.matches("^[0-9]+$")) {
                int pos = Integer.parseInt(comandoCliente) - 1;
                if (pos >= 0 && pos < totalFiltrados) {
                    int indiceProduto = listaIndicesReaisFiltrados.get(pos);
                    int reservado = obterQuantidadeReservadaNoCarrinho(indiceProduto, carrinhoIndicesProdutos, carrinhoQuantidades);
                    int saldoDisponivel = Math.max(0, tabelaProdQtdEstoque.get(indiceProduto) - reservado);

                    if (saldoDisponivel <= 0) {
                        System.out.println(CoresAnsi.erro("Sem saldo disponível.\n"));
                        continue;
                    }

                    int quantidadeDesejada = validarELerQuantidadePositiva(leitorEntrada, "Quantidade (Máx: " + saldoDisponivel + ") [0 para cancelar]: ");
                    if (quantidadeDesejada == -999) continue;

                    if (quantidadeDesejada > saldoDisponivel) {
                        System.out.println(CoresAnsi.erro("\nQuantidade solicitada (" + quantidadeDesejada + ") maior que o disponível (" + saldoDisponivel + " un).\n"));
                        continue;
                    }

                    int posExistente = carrinhoIndicesProdutos.indexOf(indiceProduto);
                    if (posExistente != -1) {
                        carrinhoQuantidades.set(posExistente, carrinhoQuantidades.get(posExistente) + quantidadeDesejada);
                    } else {
                        carrinhoIndicesProdutos.add(indiceProduto);
                        carrinhoQuantidades.add(quantidadeDesejada);
                    }
                    System.out.println(CoresAnsi.sucesso(" Adicionado ao carrinho: " + quantidadeDesejada + "x " + tabelaProdNome.get(indiceProduto) + ".\n"));
                } else {
                    System.out.println(CoresAnsi.erro("Item inválido.\n"));
                }
            } else {
                System.out.println(CoresAnsi.erro("Comando inválido.\n"));
            }
        }
    }

    // =========================================================================
    // MÓDULO 11: INICIALIZAÇÃO DE BASE E PAINEL ADMINISTRATIVO
    // Coordena a carga inicial da base operacional e o roteamento entre as
    // abas administrativas sem concentrar regras internas de cada módulo.
    // =========================================================================

    // Recarrega pré-cadastros do .TXT preservando senhas e estoques já alterados na memória
    private static void garantirPreCadastrosAtualizadosDoTxt(
            ModuloPersistenciaTxt persistencia,
            ModuloCaixa caixa,
            ModuloAutoridades moduloAutoridades,
            List<String> tabelaFuncId, List<String> tabelaFuncNome, List<String> tabelaFuncCpf,
            List<String> tabelaFuncCargo, List<String> tabelaFuncSenha, List<Double> tabelaFuncSalario,
            List<Boolean> tabelaFuncPago, List<String> tabelaFuncStatus, List<String> tabelaFuncMotivoInativo,
            List<String> tabelaFornRazaoSocial, List<String> tabelaFornCnpj, List<String> tabelaFornUf,
            List<String> tabelaFornProdutoPrincipal, List<Double> tabelaFornValorFatura,
            List<Double> tabelaFornMargemLucroPct, List<Boolean> tabelaFornFaturaPaga,
            List<String> tabelaProdNome, List<String> tabelaProdNcm, List<Double> tabelaProdPrecoVenda,
            List<Integer> tabelaProdQtdEstoque, List<String> tabelaClienteCpf, List<String> tabelaClienteNome) {

        // Guarda cópias das senhas e estoques em memória antes de reler o arquivo
        List<String> idsAnteriores = new ArrayList<>(tabelaFuncId);
        List<String> senhasAnteriores = new ArrayList<>(tabelaFuncSenha);
        List<String> nomesProdAnteriores = new ArrayList<>(tabelaProdNome);
        List<Integer> estoquesProdAnteriores = new ArrayList<>(tabelaProdQtdEstoque);

        boolean ok = persistencia.conferirECarregarPreCadastrosExternos(
                tabelaFuncId, tabelaFuncNome, tabelaFuncCpf,
                tabelaFuncCargo, tabelaFuncSenha, tabelaFuncSalario,
                tabelaFuncPago, tabelaFuncStatus, tabelaFuncMotivoInativo,
                tabelaFornRazaoSocial, tabelaFornCnpj, tabelaFornUf,
                tabelaFornProdutoPrincipal, tabelaFornValorFatura,
                tabelaFornMargemLucroPct, tabelaFornFaturaPaga,
                tabelaProdNome, tabelaProdNcm, tabelaProdPrecoVenda,
                tabelaProdQtdEstoque, tabelaClienteCpf, tabelaClienteNome
        );

        if (!ok) return;

        // Restaura senhas alteradas em memória para IDs já existentes
        for (int i = 0; i < tabelaFuncId.size(); i++) {
            int idxAntigo = idsAnteriores.indexOf(tabelaFuncId.get(i));
            if (idxAntigo != -1 && idxAntigo < senhasAnteriores.size()) {
                tabelaFuncSenha.set(i, senhasAnteriores.get(idxAntigo));
            }
        }

        // Restaura quantidades de estoque modificadas em memória para itens já existentes
        for (int i = 0; i < tabelaProdNome.size(); i++) {
            int idxAntigo = nomesProdAnteriores.indexOf(tabelaProdNome.get(i));
            if (idxAntigo != -1 && idxAntigo < estoquesProdAnteriores.size()) {
                tabelaProdQtdEstoque.set(i, estoquesProdAnteriores.get(idxAntigo));
            }
        }

        if (moduloAutoridades != null) {
            moduloAutoridades.sincronizarComTabelaFuncionarios(tabelaFuncId, tabelaFuncCargo, tabelaFuncStatus);
        }

        caixa.registrarMovimentoDados(
                "ENTRADA_DADOS",
                "Arquivo: " + ARQUIVO_PRE_CADASTROS,
                "Leitura sob demanda de pré-cadastros (" + tabelaProdNome.size() + " itens, " + tabelaFuncId.size() + " cadastros)"
        );
    }

    // Inicializa a base operacional com pré-cadastros, lançamentos iniciais, indicadores e backup
    public static void popularBaseOperacional(
            ModuloAuditoria auditoria,
            ModuloCaixa caixa,
            ModuloPersistenciaTxt persistencia,
            ModuloAutoridades moduloAutoridades,
            ModuloMovimentacaoEstoque movEstoque,
            List<String> tabelaFuncId, List<String> tabelaFuncNome, List<String> tabelaFuncCpf,
            List<String> tabelaFuncCargo, List<String> tabelaFuncSenha, List<Double> tabelaFuncSalario,
            List<Boolean> tabelaFuncPago, List<String> tabelaFuncStatus, List<String> tabelaFuncMotivoInativo,
            List<String> tabelaFornRazaoSocial, List<String> tabelaFornCnpj, List<String> tabelaFornUf,
            List<String> tabelaFornProdutoPrincipal, List<Double> tabelaFornValorFatura,
            List<Double> tabelaFornMargemLucroPct, List<Boolean> tabelaFornFaturaPaga,
            List<String> tabelaProdNome, List<String> tabelaProdNcm, List<Double> tabelaProdPrecoVenda, List<Integer> tabelaProdQtdEstoque,
            List<String> tabelaClienteCpf, List<String> tabelaClienteNome,
            List<String> tabelaTransacaoTipo, List<String> tabelaTransacaoEnvolvidos, List<String> tabelaTransacaoDescricao, List<Double> tabelaTransacaoValor,
            List<Double> indicadorReceitaBrutaVendas,
            List<Double> indicadorCustoMercadoriaVendida, List<Double> indicadorTotalFornecedoresPagos,
            List<Double> indicadorTotalSalariosPagos, List<Double> indicadorImpostosProvisionados,
            int anoExercicioFiscal) {

        garantirPreCadastrosAtualizadosDoTxt(
                persistencia, caixa, moduloAutoridades,
                tabelaFuncId, tabelaFuncNome, tabelaFuncCpf,
                tabelaFuncCargo, tabelaFuncSenha, tabelaFuncSalario,
                tabelaFuncPago, tabelaFuncStatus, tabelaFuncMotivoInativo,
                tabelaFornRazaoSocial, tabelaFornCnpj, tabelaFornUf,
                tabelaFornProdutoPrincipal, tabelaFornValorFatura,
                tabelaFornMargemLucroPct, tabelaFornFaturaPaga,
                tabelaProdNome, tabelaProdNcm, tabelaProdPrecoVenda,
                tabelaProdQtdEstoque, tabelaClienteCpf, tabelaClienteNome
        );

        tabelaTransacaoTipo.clear(); tabelaTransacaoEnvolvidos.clear(); tabelaTransacaoDescricao.clear(); tabelaTransacaoValor.clear();

        caixa.registrarMovimentoDados(
                "ENTRADA_DADOS",
                "Arquivo: " + ARQUIVO_PRE_CADASTROS,
                "Carga inicial de pré-cadastros externos"
        );

        double acumuladoReceitaVendas = 0.0;
        double acumuladoImpostos = 0.0;

        // Gera uma venda inicial de exemplo caso exista ao menos um produto
        if (!tabelaProdNome.isEmpty()) {
            int quantidadeItens = 2;
            double valorVenda = tabelaProdPrecoVenda.get(0) * quantidadeItens;
            double aliquotaIcms = calcularAliquotaIcmsPorOperacao("SP", "SP", anoExercicioFiscal);
            String nomeCliente = tabelaClienteNome.isEmpty() ? "CLIENTE AVULSO" : tabelaClienteNome.get(0);
            String cpfCliente = tabelaClienteCpf.isEmpty() ? "00000000000" : tabelaClienteCpf.get(0);

            tabelaTransacaoTipo.add("VENDA");
            tabelaTransacaoEnvolvidos.add("Cliente: " + nomeCliente + " (CPF: " + cpfCliente + ") | Caixa: TERMINAL 01");
            tabelaTransacaoDescricao.add(quantidadeItens + "x " + tabelaProdNome.get(0));
            tabelaTransacaoValor.add(valorVenda);

            if (movEstoque != null) {
                movEstoque.registrarMovimento("SAIDA_VENDA", tabelaProdNome.get(0), -quantidadeItens, tabelaProdQtdEstoque.get(0), "Cliente: " + nomeCliente, "SISTEMA");
            }

            acumuladoReceitaVendas += valorVenda;
            acumuladoImpostos += (valorVenda * aliquotaIcms);
        }

        String fornecedorInicial = tabelaFornRazaoSocial.isEmpty() ? "FORNECEDOR INICIAL" : tabelaFornRazaoSocial.get(0);
        String usuarioInicial = tabelaFuncNome.isEmpty() ? "OPERADOR INICIAL" : tabelaFuncNome.get(0);

        // Registra compra inicial e adiantamento de folha para compor o histórico base
        tabelaTransacaoTipo.add("COMPRA / ABASTECIMENTO");
        tabelaTransacaoEnvolvidos.add("Fornecedor: " + fornecedorInicial);
        tabelaTransacaoDescricao.add("Reposição inicial de lote");
        tabelaTransacaoValor.add(4500.00);

        tabelaTransacaoTipo.add("PAGAMENTO");
        tabelaTransacaoEnvolvidos.add("Funcionário: " + usuarioInicial);
        tabelaTransacaoDescricao.add("Adiantamento de folha salarial");
        tabelaTransacaoValor.add(5500.00);

        // Atualiza indicadores financeiros e ajusta o saldo inicial do caixa
        indicadorReceitaBrutaVendas.set(0, acumuladoReceitaVendas);
        indicadorCustoMercadoriaVendida.set(0, acumuladoReceitaVendas * 0.45);
        indicadorTotalFornecedoresPagos.set(0, 4500.00);
        indicadorTotalSalariosPagos.set(0, 5500.00);
        indicadorImpostosProvisionados.set(0, acumuladoImpostos);
        caixa.definirSaldo(caixa.obterSaldo() + acumuladoReceitaVendas - 10000.00);

        caixa.registrarMovimentoDados(
                "SAIDA_DADOS",
                "Arquivo: " + ARQUIVO_BACKUP_OFICIAL,
                "Gravação inicial de backup editável"
        );

        // Grava o primeiro arquivo de backup editável com o estado inicial
        persistencia.salvarBackupEditavel(
                tabelaFuncId, tabelaFuncNome, tabelaFuncCpf, tabelaFuncCargo, tabelaFuncSenha,
                tabelaFuncSalario, tabelaFuncPago, tabelaFuncStatus, tabelaFuncMotivoInativo,
                tabelaProdNome, tabelaProdNcm, tabelaProdPrecoVenda, tabelaProdQtdEstoque,
                caixa
        );

        auditoria.registrar("SISTEMA", "Base inicializada via '" + ARQUIVO_PRE_CADASTROS + "' e salva em '" + ARQUIVO_BACKUP_OFICIAL + "'");
        System.out.println(CoresAnsi.sucesso(" Base inicializada (" + tabelaProdNome.size() + " itens, " + tabelaTransacaoTipo.size() + " lançamentos)."));
        System.out.println(CoresAnsi.colorir(CoresAnsi.CIANO, " Pré-cadastros externos em: '" + ARQUIVO_PRE_CADASTROS + "'"));
        System.out.println(CoresAnsi.colorir(CoresAnsi.CIANO, " Arquivo editável disponível: '" + ARQUIVO_BACKUP_OFICIAL + "'"));
        System.out.println(CoresAnsi.colorir(CoresAnsi.CIANO, " Log de entradas/saídas em: '" + ARQUIVO_LOG_DADOS + "'\n"));
    }

    // Painel principal do usuário autenticado, exibindo opções conforme autoridades ativas
    public static void exibirMenuPainelAdministrativo(
            Scanner leitorEntrada,
            ModuloSessao sessao,
            ModuloAuditoria auditoria,
            ModuloCaixa caixa,
            ModuloLogDadosTxt logDados,
            ModuloPersistenciaTxt persistencia,
            ModuloAutoridades moduloAutoridades,
            ModuloMovimentacaoEstoque movEstoque,
            List<String> tabelaFuncId, List<String> tabelaFuncNome, List<String> tabelaFuncCpf,
            List<String> tabelaFuncCargo, List<String> tabelaFuncSenha, List<Double> tabelaFuncSalario,
            List<Boolean> tabelaFuncPago, List<String> tabelaFuncStatus, List<String> tabelaFuncMotivoInativo,
            List<String> tabelaFornRazaoSocial, List<String> tabelaFornCnpj, List<String> tabelaFornUf,
            List<String> tabelaFornProdutoPrincipal, List<Double> tabelaFornValorFatura,
            List<Double> tabelaFornMargemLucroPct, List<Boolean> tabelaFornFaturaPaga,
            List<String> tabelaProdNome, List<String> tabelaProdNcm, List<Double> tabelaProdPrecoVenda, List<Integer> tabelaProdQtdEstoque,
            List<String> tabelaClienteCpf, List<String> tabelaClienteNome,
            List<Double> indicadorGastosEstoque, List<Double> indicadorReceitaBrutaVendas,
            List<Double> indicadorCustoMercadoriaVendida, List<Double> indicadorTotalSalariosPagos,
            List<Double> indicadorTotalFornecedoresPagos, List<Double> indicadorImpostosProvisionados,
            List<String> tabelaTransacaoTipo, List<String> tabelaTransacaoEnvolvidos,
            List<String> tabelaTransacaoDescricao, List<Double> tabelaTransacaoValor) {

        while (sessao.validarOuRevogarSessao()) {
            System.out.println(CoresAnsi.titulo("\n================================================================"));
            System.out.println(CoresAnsi.titulo("                      PAINEL ADMINISTRATIVO                     "));
            System.out.println(CoresAnsi.titulo("================================================================"));
            System.out.println("[0] Meu Perfil");
            if (sessao.temAutoridade(Autoridade.RH_VER_FUNCIONARIOS)) {
                System.out.println("[1] Opção 1 (RH / Funcionários)");
            }
            if (sessao.temAutoridade(Autoridade.FORN_VER_LISTA)) {
                System.out.println("[2] Opção 2 (Fornecedores)");
            }
            if (sessao.temAutoridade(Autoridade.EST_VER_CATALOGO)
                    || sessao.temAutoridade(Autoridade.FIN_VER_VALORES_SENSIVEIS)
                    || sessao.temAutoridade(Autoridade.LOG_VER_E_EXPORTAR)
                    || sessao.temAutoridade(Autoridade.SIS_AUDITORIA_E_BACKUP)) {
                System.out.println("[3] Opção 3 (Estoque, Financeiro e Logs)");
            }
            System.out.println("[4] Sair");
            System.out.println(CoresAnsi.titulo("================================================================"));

            int opcaoPainel = validarELerNumeroInteiro(leitorEntrada, "Selecione: ");
            if (opcaoPainel == 0) {
                exibirAbaMeuPerfil(
                        leitorEntrada, sessao, auditoria, caixa, persistencia,
                        tabelaFuncId, tabelaFuncNome, tabelaFuncCpf,
                        tabelaFuncCargo, tabelaFuncSenha,
                        tabelaFuncSalario, tabelaFuncPago,
                        tabelaFuncStatus, tabelaFuncMotivoInativo,
                        tabelaProdNome, tabelaProdNcm, tabelaProdPrecoVenda,
                        tabelaProdQtdEstoque
                );
            } else if (opcaoPainel == 1) {
                exibirMenuGestaoFuncionarios(
                        leitorEntrada, sessao, auditoria, caixa, persistencia, moduloAutoridades,
                        tabelaFuncId, tabelaFuncNome, tabelaFuncCpf, tabelaFuncCargo,
                        tabelaFuncSenha, tabelaFuncSalario, tabelaFuncPago, tabelaFuncStatus, tabelaFuncMotivoInativo,
                        tabelaFornRazaoSocial, tabelaFornCnpj, tabelaFornUf, tabelaFornProdutoPrincipal,
                        tabelaFornValorFatura, tabelaFornMargemLucroPct, tabelaFornFaturaPaga,
                        tabelaProdNome, tabelaProdNcm, tabelaProdPrecoVenda, tabelaProdQtdEstoque,
                        tabelaClienteCpf, tabelaClienteNome,
                        indicadorTotalSalariosPagos
                );
            } else if (opcaoPainel == 2) {
                exibirMenuGestaoFornecedores(
                        leitorEntrada, sessao, auditoria, caixa, persistencia, moduloAutoridades, movEstoque,
                        tabelaFuncId, tabelaFuncNome, tabelaFuncCpf, tabelaFuncCargo,
                        tabelaFuncSenha, tabelaFuncSalario, tabelaFuncPago, tabelaFuncStatus, tabelaFuncMotivoInativo,
                        tabelaFornRazaoSocial, tabelaFornCnpj, tabelaFornUf,
                        tabelaFornProdutoPrincipal, tabelaFornValorFatura, tabelaFornMargemLucroPct, tabelaFornFaturaPaga,
                        tabelaProdNome, tabelaProdNcm, tabelaProdPrecoVenda, tabelaProdQtdEstoque,
                        tabelaClienteCpf, tabelaClienteNome,
                        indicadorGastosEstoque, indicadorTotalFornecedoresPagos
                );
            } else if (opcaoPainel == 3) {
                exibirMenuFinanceiroEstoqueEHistorico(
                        leitorEntrada, sessao, auditoria, caixa, logDados, persistencia, moduloAutoridades, movEstoque,
                        tabelaProdNome, tabelaProdNcm, tabelaProdPrecoVenda, tabelaProdQtdEstoque,
                        tabelaClienteCpf, tabelaClienteNome, indicadorGastosEstoque, indicadorReceitaBrutaVendas,
                        indicadorCustoMercadoriaVendida, indicadorTotalSalariosPagos,
                        indicadorTotalFornecedoresPagos, indicadorImpostosProvisionados,
                        tabelaTransacaoTipo, tabelaTransacaoEnvolvidos, tabelaTransacaoDescricao, tabelaTransacaoValor,
                        tabelaFuncId, tabelaFuncNome, tabelaFuncCpf, tabelaFuncCargo, tabelaFuncSenha,
                        tabelaFuncSalario, tabelaFuncPago, tabelaFuncStatus, tabelaFuncMotivoInativo,
                        tabelaFornRazaoSocial, tabelaFornCnpj, tabelaFornUf, tabelaFornProdutoPrincipal,
                        tabelaFornValorFatura, tabelaFornMargemLucroPct, tabelaFornFaturaPaga
                );
            } else if (opcaoPainel == 4 || opcaoPainel == -999) {
                break;
            } else {
                System.out.println(CoresAnsi.erro("Opção indisponível.\n"));
            }
        }
    }

    // Ponto de entrada: inicializa estruturas, carrega base e executa o loop de acesso
    public static void main(String[] args) {
        // Garante o fechamento automático do Scanner ao encerrar o programa
        try (Scanner leitorEntrada = new Scanner(System.in)) {

            System.out.println(CoresAnsi.titulo("\n================================================================"));
            System.out.println(CoresAnsi.titulo("            BEM-VINDO AO PROTEI CREAT (L'ESTOQUE)               "));
            System.out.println(CoresAnsi.titulo("       SISTEMA MODULAR DE GESTÃO TRIBUTÁRIA E OPERACIONAL       "));
            System.out.println(CoresAnsi.titulo("================================================================"));

            // Tabelas paralelas de funcionários
            List<String> tabelaFuncId = new ArrayList<>();
            List<String> tabelaFuncNome = new ArrayList<>();
            List<String> tabelaFuncCpf = new ArrayList<>();
            List<String> tabelaFuncCargo = new ArrayList<>();
            List<String> tabelaFuncSenha = new ArrayList<>();
            List<Double> tabelaFuncSalario = new ArrayList<>();
            List<Boolean> tabelaFuncPago = new ArrayList<>();
            List<String> tabelaFuncStatus = new ArrayList<>();
            List<String> tabelaFuncMotivoInativo = new ArrayList<>();

            // Tabelas paralelas de fornecedores
            List<String> tabelaFornRazaoSocial = new ArrayList<>();
            List<String> tabelaFornCnpj = new ArrayList<>();
            List<String> tabelaFornUf = new ArrayList<>();
            List<String> tabelaFornProdutoPrincipal = new ArrayList<>();
            List<Double> tabelaFornValorFatura = new ArrayList<>();
            List<Double> tabelaFornMargemLucroPct = new ArrayList<>();
            List<Boolean> tabelaFornFaturaPaga = new ArrayList<>();

            // Tabelas paralelas de clientes
            List<String> tabelaClienteCpf = new ArrayList<>();
            List<String> tabelaClienteNome = new ArrayList<>();

            // Tabelas paralelas de produtos
            List<String> tabelaProdNome = new ArrayList<>();
            List<String> tabelaProdNcm = new ArrayList<>();
            List<Double> tabelaProdPrecoVenda = new ArrayList<>();
            List<Integer> tabelaProdQtdEstoque = new ArrayList<>();

            // Tabelas paralelas de transações (financeiro e entradas/saídas de dados)
            List<String> tabelaTransacaoTipo = new ArrayList<>();
            List<String> tabelaTransacaoEnvolvidos = new ArrayList<>();
            List<String> tabelaTransacaoDescricao = new ArrayList<>();
            List<Double> tabelaTransacaoValor = new ArrayList<>();

            // Indicadores em lista de 1 elemento para permitir mutação por referência entre métodos
            List<Double> indicadorSaldoCaixaGlobal = new ArrayList<>(Arrays.asList(200000.00));
            List<Double> indicadorGastosEstoque = new ArrayList<>(Arrays.asList(35000.00));
            List<Double> indicadorReceitaBrutaVendas = new ArrayList<>(Arrays.asList(0.0));
            List<Double> indicadorCustoMercadoriaVendida = new ArrayList<>(Arrays.asList(0.0));
            List<Double> indicadorTotalSalariosPagos = new ArrayList<>(Arrays.asList(0.0));
            List<Double> indicadorTotalFornecedoresPagos = new ArrayList<>(Arrays.asList(0.0));
            List<Double> indicadorImpostosProvisionados = new ArrayList<>(Arrays.asList(0.0));
            int anoExercicioFiscal = 2026;

            // Instancia os módulos do sistema
            ModuloAutoridades moduloAutoridades = new ModuloAutoridades();
            ModuloMovimentacaoEstoque movEstoque = new ModuloMovimentacaoEstoque();
            ModuloLogDadosTxt logDados = new ModuloLogDadosTxt(ARQUIVO_LOG_DADOS);
            ModuloAuditoria auditoria = new ModuloAuditoria();
            ModuloCaixa caixa = new ModuloCaixa(indicadorSaldoCaixaGlobal, tabelaTransacaoTipo, tabelaTransacaoEnvolvidos, tabelaTransacaoDescricao, tabelaTransacaoValor, logDados);
            ModuloPersistenciaTxt persistencia = new ModuloPersistenciaTxt(ARQUIVO_PRE_CADASTROS, ARQUIVO_BACKUP_OFICIAL);

            // Popula dados iniciais e salva o primeiro backup editável
            popularBaseOperacional(
                    auditoria, caixa, persistencia, moduloAutoridades, movEstoque,
                    tabelaFuncId, tabelaFuncNome, tabelaFuncCpf, tabelaFuncCargo, tabelaFuncSenha,
                    tabelaFuncSalario, tabelaFuncPago, tabelaFuncStatus, tabelaFuncMotivoInativo,
                    tabelaFornRazaoSocial, tabelaFornCnpj, tabelaFornUf, tabelaFornProdutoPrincipal,
                    tabelaFornValorFatura, tabelaFornMargemLucroPct, tabelaFornFaturaPaga,
                    tabelaProdNome, tabelaProdNcm, tabelaProdPrecoVenda, tabelaProdQtdEstoque,
                    tabelaClienteCpf, tabelaClienteNome,
                    tabelaTransacaoTipo, tabelaTransacaoEnvolvidos, tabelaTransacaoDescricao, tabelaTransacaoValor,
                    indicadorReceitaBrutaVendas, indicadorCustoMercadoriaVendida,
                    indicadorTotalFornecedoresPagos, indicadorTotalSalariosPagos, indicadorImpostosProvisionados,
                    anoExercicioFiscal
            );

            boolean execucaoSistemaAtiva = true;
            while (execucaoSistemaAtiva) {
                System.out.println(CoresAnsi.titulo("\n================================================================"));
                System.out.println(CoresAnsi.titulo("                 MENU PRINCIPAL DE ACESSO                       "));
                System.out.println(CoresAnsi.titulo("================================================================"));
                System.out.println("[1] Portal do Cliente");
                System.out.println("[2] Acesso Administrativo");
                System.out.println("[3] Encerrar Sistema");
                System.out.println(CoresAnsi.titulo("================================================================"));

                int opcaoAcesso = validarELerNumeroInteiro(leitorEntrada, "Opção: ");
                if (opcaoAcesso == 1) {
                    executarPortalComprasCliente(
                            leitorEntrada, caixa, movEstoque,
                            tabelaProdNome, tabelaProdNcm, tabelaProdPrecoVenda, tabelaProdQtdEstoque,
                            indicadorReceitaBrutaVendas, indicadorCustoMercadoriaVendida, indicadorImpostosProvisionados,
                            tabelaClienteCpf, tabelaClienteNome,
                            tabelaTransacaoTipo, tabelaTransacaoEnvolvidos, tabelaTransacaoDescricao, tabelaTransacaoValor,
                            anoExercicioFiscal
                    );
                } else if (opcaoAcesso == 2) {
                    if (tabelaFuncId.isEmpty()) {
                        garantirPreCadastrosAtualizadosDoTxt(
                                persistencia, caixa, moduloAutoridades,
                                tabelaFuncId, tabelaFuncNome, tabelaFuncCpf,
                                tabelaFuncCargo, tabelaFuncSenha, tabelaFuncSalario,
                                tabelaFuncPago, tabelaFuncStatus, tabelaFuncMotivoInativo,
                                tabelaFornRazaoSocial, tabelaFornCnpj, tabelaFornUf,
                                tabelaFornProdutoPrincipal, tabelaFornValorFatura,
                                tabelaFornMargemLucroPct, tabelaFornFaturaPaga,
                                tabelaProdNome, tabelaProdNcm, tabelaProdPrecoVenda,
                                tabelaProdQtdEstoque, tabelaClienteCpf, tabelaClienteNome
                        );
                    }

                    System.out.println(CoresAnsi.titulo("\n=== AUTENTICAÇÃO ==="));
                    System.out.print("Identificação [0 para voltar]: ");
                    String idDigitado = lerLinhaSegura(leitorEntrada).toUpperCase();
                    if (isComandoCancelamento(idDigitado)) continue;

                    // Permite até 3 tentativas de senha para o ID informado
                    boolean loginAutenticado = false;
                    for (int tentativa = 1; tentativa <= 3; tentativa++) {
                        System.out.print("Senha (Tentativa " + tentativa + "/3): ");
                        String senhaDigitada = lerLinhaSegura(leitorEntrada);

                        int indiceAtual = tabelaFuncId.indexOf(idDigitado);
                        boolean usuarioValidoEAtivo = (indiceAtual != -1)
                                && tabelaFuncStatus.get(indiceAtual).equalsIgnoreCase("ATIVO");

                        if (usuarioValidoEAtivo && tabelaFuncSenha.get(indiceAtual).equals(senhaDigitada)) {
                            loginAutenticado = true;
                            auditoria.registrar(tabelaFuncNome.get(indiceAtual), "Login realizado com sucesso");
                            System.out.println(CoresAnsi.sucesso("\n Acesso autorizado.\n"));
                            break;
                        }
                        System.out.println(CoresAnsi.erro("Credenciais inválidas.\n"));
                    }

                    if (loginAutenticado) {
                        ModuloSessao sessao = new ModuloSessao(
                                idDigitado, tabelaFuncId, tabelaFuncNome, tabelaFuncCargo, tabelaFuncSenha, tabelaFuncStatus, moduloAutoridades
                        );

                        // Abre diretamente o painel administrativo após autenticar; Meu Perfil fica acessível pela opção [0]
                        exibirMenuPainelAdministrativo(
                                leitorEntrada, sessao, auditoria, caixa, logDados, persistencia, moduloAutoridades, movEstoque,
                                tabelaFuncId, tabelaFuncNome, tabelaFuncCpf, tabelaFuncCargo, tabelaFuncSenha,
                                tabelaFuncSalario, tabelaFuncPago, tabelaFuncStatus, tabelaFuncMotivoInativo,
                                tabelaFornRazaoSocial, tabelaFornCnpj, tabelaFornUf, tabelaFornProdutoPrincipal,
                                tabelaFornValorFatura, tabelaFornMargemLucroPct, tabelaFornFaturaPaga,
                                tabelaProdNome, tabelaProdNcm, tabelaProdPrecoVenda, tabelaProdQtdEstoque,
                                tabelaClienteCpf, tabelaClienteNome,
                                indicadorGastosEstoque, indicadorReceitaBrutaVendas,
                                indicadorCustoMercadoriaVendida, indicadorTotalSalariosPagos,
                                indicadorTotalFornecedoresPagos, indicadorImpostosProvisionados,
                                tabelaTransacaoTipo, tabelaTransacaoEnvolvidos, tabelaTransacaoDescricao, tabelaTransacaoValor
                        );
                    }
                } else if (opcaoAcesso == 3 || opcaoAcesso == -999) {
                    execucaoSistemaAtiva = false;
                    System.out.println(CoresAnsi.colorir(CoresAnsi.CIANO, "\n[SISTEMA] Encerrado com segurança.\n"));
                } else {
                    System.out.println(CoresAnsi.erro("Opção inválida.\n"));
                }
            }
        }
    }
}
