// Permite armazenar conjuntos de constantes de um enum de forma compacta e rápida
import java.util.EnumSet;
// Estrutura de mapa chave-valor para vincular o ID do funcionário ao seu conjunto de autoridades
import java.util.HashMap;
// Mapa que mantém a ordem de inserção, útil para consolidar históricos por cliente
import java.util.LinkedHashMap;
// Representa um conjunto sem elementos repetidos para consultas de permissão
import java.util.Set;
// Utilitários de lista, arrays e leitura de dados do console
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
// Classes de I/O para leitura e gravação de arquivos .TXT
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
// Data e hora atuais e formatador para carimbo de tempo em logs e exportações
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Classe principal do protótipo modular do sistema Protei Creat (L'Estoque)
public class prototipo {

    // Caminho do arquivo externo de pré-cadastros (funcionários, fornecedores, produtos e clientes)
    public static final String ARQUIVO_PRE_CADASTROS = "dados_pre_cadastros.txt";
    // Caminho do arquivo editável de backup oficial do sistema
    public static final String ARQUIVO_BACKUP_OFICIAL = "protei_creat_backup_oficial.txt";
    // Caminho do arquivo gerado automaticamente antes de restaurar um backup (ponto de rollback)
    public static final String ARQUIVO_ROLLBACK_AUTO = "protei_creat_rollback_auto.txt";
    // Caminho do arquivo de log contínuo de entradas e saídas de dados
    public static final String ARQUIVO_LOG_DADOS = "registro_entradas_saidas_dados.txt";
    // Nome padrão do arquivo ao exportar eventos filtrados do log
    public static final String ARQUIVO_LOG_FILTRADO_EXPORTADO = "log_entradas_saidas_filtrado.txt";
    // Quantidade limite para sinalizar item com estoque baixo na listagem
    public static final int ESTOQUE_MINIMO_ALERTA = 10;

    // Padroniza o formato de data e hora usado em logs e exportações
    private static final DateTimeFormatter FORMATADOR_DATA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Enumeração que representa cada permissão granular disponível no sistema
    enum Autoridade {
        RH_VER_FUNCIONARIOS("Visualizar lista de funcionários"),
        RH_GERIR_CADASTROS("Cadastrar, editar e alterar status de funcionários"),
        RH_RESETAR_SENHA("Redefinir senha de funcionário para temporária"),
        RH_GERIR_AUTORIDADES("Conceder ou revogar autoridades de funcionários"),
        FORN_VER_LISTA("Visualizar registros de fornecedores"),
        FORN_GERIR_OPERACOES("Registrar compras, editar ou remover fornecedores"),
        EST_VER_CATALOGO("Visualizar catálogo e saldos de estoque"),
        EST_GERIR_CATALOGO("Cadastrar itens, editar preços, ajustar saldos e ver histórico de estoque"),
        FIN_VER_VALORES_SENSIVEIS("Visualizar salários, caixa, lançamentos e demonstrativos financeiros"),
        FIN_PAGAR_E_ESTORNAR("Registrar pagamentos/quitações e estornar lançamentos"),
        LOG_VER_E_EXPORTAR("Consultar, filtrar e exportar logs de entradas/saídas"),
        SIS_AUDITORIA_E_BACKUP("Acessar auditoria, salvar backup e restaurar arquivos .TXT");

        // Descrição amigável exibida na tela de gerenciamento de autoridades
        private final String descricao;

        // Construtor do enum que associa a descrição à constante
        Autoridade(String descricao) {
            this.descricao = descricao;
        }

        // Retorna a descrição legível da autoridade
        String getDescricao() {
            return descricao;
        }
    }

    // Gerencia o conjunto de autoridades de cada funcionário por ID
    static class ModuloAutoridades {
        // Mapeia o ID do funcionário (em maiúsculas) para seu EnumSet de autoridades
        private final Map<String, EnumSet<Autoridade>> autoridadesPorId = new HashMap<>();

        // Define o conjunto inicial de autoridades de acordo com o cargo
        static EnumSet<Autoridade> autoridadesPadraoPorCargo(String cargo) {
            // Se o cargo for nulo, não concede nenhuma autoridade
            if (cargo == null) return EnumSet.noneOf(Autoridade.class);
            // Seleciona as permissões padrão conforme o perfil/cargo
            switch (cargo.trim().toUpperCase()) {
                case "GERENTE":
                    // Gerente recebe todas as autoridades cadastradas
                    return EnumSet.allOf(Autoridade.class);
                case "FINANCEIRO":
                    // Financeiro recebe permissões de RH consulta, fornecedores, estoque, financeiro e logs
                    return EnumSet.of(
                            Autoridade.RH_VER_FUNCIONARIOS,
                            Autoridade.FORN_VER_LISTA,
                            Autoridade.FORN_GERIR_OPERACOES,
                            Autoridade.EST_VER_CATALOGO,
                            Autoridade.EST_GERIR_CATALOGO,
                            Autoridade.FIN_VER_VALORES_SENSIVEIS,
                            Autoridade.FIN_PAGAR_E_ESTORNAR,
                            Autoridade.LOG_VER_E_EXPORTAR
                    );
                case "OPERADOR":
                    // Operador recebe permissões básicas de visualização de estoque/fornecedores e logs
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

        // Mantém o mapa de autoridades alinhado com a lista de funcionários e seus status
        void sincronizarComTabelaFuncionarios(List<String> ids, List<String> cargos, List<String> status) {
            // Cria um conjunto com os IDs atualmente existentes para remover órfãos
            Set<String> idsAtuais = new java.util.HashSet<>(ids);
            // Remove entradas de IDs que não existem mais na tabela de funcionários
            autoridadesPorId.keySet().retainAll(idsAtuais);

            // Percorre todos os funcionários cadastrados
            for (int i = 0; i < ids.size(); i++) {
                String id = ids.get(i);
                // Verifica se o funcionário está com status ATIVO
                boolean ativo = i < status.size() && "ATIVO".equalsIgnoreCase(status.get(i));
                if (!ativo) {
                    // Funcionário inativo/demitido tem todas as autoridades zeradas
                    autoridadesPorId.put(id, EnumSet.noneOf(Autoridade.class));
                } else if (!autoridadesPorId.containsKey(id)) {
                    // Se o funcionário ativo ainda não tem entrada, aplica o padrão do cargo
                    String cargo = i < cargos.size() ? cargos.get(i) : "OPERADOR";
                    autoridadesPorId.put(id, autoridadesPadraoPorCargo(cargo));
                }
            }
        }

        // Restaura autoridades lidas do arquivo de backup para um funcionário ativo
        void aplicarAutoridadesCarregadas(String idUsuario, boolean ativo, List<String> nomesAutoridades) {
            // Ignora usuários inativos ou IDs vazios
            if (!ativo || idUsuario == null || idUsuario.isBlank()) return;
            // Cria um conjunto vazio para popular com os nomes válidos
            EnumSet<Autoridade> conjunto = EnumSet.noneOf(Autoridade.class);
            for (String nome : nomesAutoridades) {
                if (nome == null || nome.isBlank()) continue;
                try {
                    // Converte o texto do arquivo para a constante Autoridade correspondente
                    conjunto.add(Autoridade.valueOf(nome.trim().toUpperCase()));
                } catch (IllegalArgumentException ignored) {
                    // Ignora nomes de autoridades obsoletos ou inválidos no arquivo
                }
            }
            // Grava o conjunto restaurado no mapa
            autoridadesPorId.put(idUsuario.trim().toUpperCase(), conjunto);
        }

        // Obtém o conjunto de autoridades vigentes do usuário (vazio se inativo)
        EnumSet<Autoridade> obterAutoridades(String idUsuario, boolean ativo) {
            if (!ativo || idUsuario == null) return EnumSet.noneOf(Autoridade.class);
            // Busca ou inicializa um conjunto vazio caso ainda não exista
            EnumSet<Autoridade> conjunto = autoridadesPorId.get(idUsuario);
            if (conjunto == null) {
                conjunto = EnumSet.noneOf(Autoridade.class);
                autoridadesPorId.put(idUsuario, conjunto);
            }
            return conjunto;
        }

        // Verifica se o usuário ativo possui uma autoridade específica
        boolean temAutoridade(String idUsuario, boolean ativo, Autoridade autoridade) {
            return obterAutoridades(idUsuario, ativo).contains(autoridade);
        }

        // Concede ou revoga individualmente uma autoridade para o ID informado
        void definirAutoridade(String idUsuario, Autoridade autoridade, boolean conceder) {
            EnumSet<Autoridade> conjunto = autoridadesPorId.computeIfAbsent(idUsuario, k -> EnumSet.noneOf(Autoridade.class));
            if (conceder) {
                conjunto.add(autoridade);
            } else {
                conjunto.remove(autoridade);
            }
        }

        // Redefine todas as autoridades de um usuário conforme seu status e cargo
        void redefinirPorCargo(String idUsuario, String cargo, boolean ativo) {
            if (!ativo) {
                autoridadesPorId.put(idUsuario, EnumSet.noneOf(Autoridade.class));
            } else {
                autoridadesPorId.put(idUsuario, autoridadesPadraoPorCargo(cargo));
            }
        }
    }

    // Centraliza códigos de cor ANSI para destacar mensagens no console
    static class CoresAnsi {
        static final String RESET = "\u001B[0m";
        static final String VERMELHO = "\u001B[31m";
        static final String VERDE = "\u001B[32m";
        static final String AMARELO = "\u001B[33m";
        static final String AZUL = "\u001B[34m";
        static final String CIANO = "\u001B[36m";
        static final String NEGRITO_CIANO = "\u001B[1;36m";

        // Envolve um texto com a cor escolhida e reseta ao final
        static String colorir(String cor, String texto) {
            if (texto == null) return "";
            return cor + texto + RESET;
        }

        // Formata mensagens de sucesso em verde
        static String sucesso(String texto) {
            return colorir(VERDE, texto);
        }

        // Formata mensagens de erro/bloqueio em vermelho
        static String erro(String texto) {
            return colorir(VERMELHO, texto);
        }

        // Formata mensagens de aviso/atenção em amarelo
        static String aviso(String texto) {
            return colorir(AMARELO, texto);
        }

        // Formata títulos e molduras em ciano negrito
        static String titulo(String texto) {
            return colorir(NEGRITO_CIANO, texto);
        }
    }

    // Registra e consulta movimentações de quantidade dos itens em estoque
    static class ModuloMovimentacaoEstoque {
        // Lista em memória das linhas formatadas de movimentação de estoque
        private final List<String> registros = new ArrayList<>();

        // Adiciona um evento de movimentação com variação (+ / -), saldo final e responsável
        void registrarMovimento(String tipoMovimento, String produto, int deltaQuantidade, int saldoFinal, String origemDestino, String responsavel) {
            String dataHora = LocalDateTime.now().format(FORMATADOR_DATA_HORA);
            // Garante sinal explícito de '+' para entradas/ajustes positivos
            String sinal = deltaQuantidade >= 0 ? "+" + deltaQuantidade : String.valueOf(deltaQuantidade);
            String linha = String.format("[%s] | %s | Item: %s | Var: %s un | Saldo: %d un | Ref: %s | Resp: %s",
                    dataHora,
                    sanitizarCampoTxt(tipoMovimento),
                    sanitizarCampoTxt(produto),
                    sinal,
                    Math.max(0, saldoFinal),
                    sanitizarCampoTxt(origemDestino),
                    sanitizarCampoTxt(responsavel));
            registros.add(linha);
        }

        // Filtra os registros de movimentação por termo (case-insensitive)
        List<String> buscarFiltrados(String termoFiltro) {
            List<String> resultado = new ArrayList<>();
            String filtro = (termoFiltro == null) ? "" : termoFiltro.trim().toUpperCase();
            for (String linha : registros) {
                if (filtro.isEmpty() || linha.toUpperCase().contains(filtro)) {
                    resultado.add(linha);
                }
            }
            return resultado;
        }
    }

    // Grava e lê o log persistente de entradas e saídas de dados em arquivo .TXT
    static class ModuloLogDadosTxt {
        private final String caminhoArquivoLog;

        // Inicializa o módulo e garante a criação do cabeçalho se o arquivo não existir
        ModuloLogDadosTxt(String caminhoArquivoLog) {
            this.caminhoArquivoLog = caminhoArquivoLog;
            garantirCabecalho();
        }

        // Escreve o cabeçalho explicativo apenas quando o arquivo está vazio ou ausente
        private void garantirCabecalho() {
            File arquivo = new File(caminhoArquivoLog);
            if (arquivo.exists() && arquivo.length() > 0) return;

            try (BufferedWriter escritor = new BufferedWriter(new FileWriter(arquivo, true))) {
                escritor.write("================================================================================\n");
                escritor.write("# LOG DEDICADO DE ENTRADAS E SAÍDAS DE DADOS - PROTEI CREAT (L'ESTOQUE)\n");
                escritor.write("# Formato: [DATA HORA] | [DIRECAO] | Envolvidos: ... | Detalhes: ...\n");
                escritor.write("================================================================================\n");
            } catch (IOException ignored) {
                // Evita interromper o fluxo do console caso o disco esteja somente leitura
            }
        }

        // Acrescenta uma nova linha de evento ao arquivo de log
        void gravarEvento(String direcao, String envolvidos, String descricao) {
            garantirCabecalho();
            String timestamp = LocalDateTime.now().format(FORMATADOR_DATA_HORA);
            String dirLimpa = sanitizarCampoTxt(direcao);
            String envLimpo = sanitizarCampoTxt(envolvidos);
            String descLimpa = sanitizarCampoTxt(descricao);

            String linhaFormatada = String.format("[%s] | [%s] | Envolvidos: %s | Detalhes: %s",
                    timestamp, dirLimpa, envLimpo, descLimpa);

            try (BufferedWriter escritor = new BufferedWriter(new FileWriter(caminhoArquivoLog, true))) {
                escritor.write(linhaFormatada + "\n");
            } catch (IOException ignored) {
            }
        }

        // Lê as linhas úteis do arquivo de log aplicando um filtro opcional
        List<String> lerEventosFiltrados(String termoFiltro) {
            garantirCabecalho();
            List<String> eventos = new ArrayList<>();
            String filtroNormalizado = (termoFiltro == null) ? "" : termoFiltro.trim().toUpperCase();

            File arquivo = new File(caminhoArquivoLog);
            if (!arquivo.exists()) return eventos;

            try (BufferedReader leitor = new BufferedReader(new FileReader(arquivo))) {
                String linha;
                while ((linha = leitor.readLine()) != null) {
                    String limpa = linha.trim();
                    // Ignora linhas vazias, comentários '#' e separadores '='
                    if (limpa.isEmpty() || limpa.startsWith("#") || limpa.startsWith("=")) continue;
                    if (filtroNormalizado.isEmpty() || limpa.toUpperCase().contains(filtroNormalizado)) {
                        eventos.add(limpa);
                    }
                }
            } catch (IOException ignored) {
            }
            return eventos;
        }
    }

    // Registra ações administrativas e operacionais para consulta na auditoria
    static class ModuloAuditoria {
        private final List<String> registros = new ArrayList<>();

        // Registra um evento com nome do operador e descrição da ação
        void registrar(String operador, String acao) {
            String autor = (operador == null || operador.isBlank()) ? "SISTEMA" : sanitizarCampoTxt(operador.trim());
            String desc = (acao == null) ? "" : sanitizarCampoTxt(acao.trim());
            registros.add("[AUDITORIA] Operador: " + autor + " | Ação: " + desc);
        }

        // Retorna eventos filtrados por palavra-chave
        List<String> buscarFiltrados(String termoFiltro) {
            List<String> resultado = new ArrayList<>();
            String filtroNormalizado = (termoFiltro == null) ? "" : termoFiltro.trim().toUpperCase();
            for (String evento : registros) {
                if (filtroNormalizado.isEmpty() || evento.toUpperCase().contains(filtroNormalizado)) {
                    resultado.add(evento);
                }
            }
            return resultado;
        }

        // Indica se não há registros de auditoria ainda
        boolean estaVazio() {
            return registros.isEmpty();
        }
    }

    // Representa a sessão do usuário autenticado e valida permissões/status em tempo real
    static class ModuloSessao {
        private final String idUsuario;
        private final List<String> tabelaId;
        private final List<String> tabelaNome;
        private final List<String> tabelaCargo;
        private final List<String> tabelaSenha;
        private final List<String> tabelaStatus;
        private final ModuloAutoridades moduloAutoridades;

        // Construtor que vincula a sessão às tabelas de funcionários e ao módulo de autoridades
        ModuloSessao(String idUsuario,
                     List<String> tabelaId,
                     List<String> tabelaNome,
                     List<String> tabelaCargo,
                     List<String> tabelaSenha,
                     List<String> tabelaStatus,
                     ModuloAutoridades moduloAutoridades) {
            this.idUsuario = idUsuario;
            this.tabelaId = tabelaId;
            this.tabelaNome = tabelaNome;
            this.tabelaCargo = tabelaCargo;
            this.tabelaSenha = tabelaSenha;
            this.tabelaStatus = tabelaStatus;
            this.moduloAutoridades = moduloAutoridades;
        }

        // Localiza o índice atual do usuário na tabela de IDs
        private int indiceAtual() {
            return tabelaId.indexOf(idUsuario);
        }

        // Verifica se o usuário ainda existe e está com status ATIVO
        boolean estaAtiva() {
            int idx = indiceAtual();
            return idx != -1 && "ATIVO".equalsIgnoreCase(tabelaStatus.get(idx));
        }

        // Valida se a sessão continua ativa; se foi inativada durante o uso, exibe aviso e encerra
        boolean validarOuRevogarSessao() {
            if (estaAtiva()) return true;
            System.out.println(CoresAnsi.erro("\n [REVOGAÇÃO DE SESSÃO]: Sua autoridade foi encerrada ou o registro está inativo.\n"));
            return false;
        }

        // Retorna o ID do usuário logado
        String obterIdVigente() {
            return idUsuario;
        }

        // Retorna o nome atualizado do usuário logado
        String obterNomeVigente() {
            int idx = indiceAtual();
            return (idx != -1) ? tabelaNome.get(idx) : "DESCONHECIDO";
        }

        // Retorna o cargo atualizado do usuário logado
        String obterCargoVigente() {
            int idx = indiceAtual();
            return (idx != -1) ? tabelaCargo.get(idx) : "SEM_CARGO";
        }

        // Retorna a senha atual do usuário logado (usada para confirmação de ações críticas)
        String obterSenhaVigente() {
            int idx = indiceAtual();
            return (idx != -1) ? tabelaSenha.get(idx) : "";
        }

        // Consulta se o usuário ativo possui determinada autoridade
        boolean temAutoridade(Autoridade autoridade) {
            return moduloAutoridades != null && moduloAutoridades.temAutoridade(idUsuario, estaAtiva(), autoridade);
        }

        // Exibe o valor monetário formatado ou "[RESTRITO]" caso falte permissão sensível
        String exibirValorCondicional(Autoridade autoridadeNecessaria, double valor) {
            if (temAutoridade(autoridadeNecessaria)) {
                return formatarValorMonetarioReais(valor);
            }
            return CoresAnsi.aviso("[RESTRITO]");
        }

        // Exige uma autoridade e imprime mensagem de erro caso o usuário não a possua
        boolean exigirAutoridade(Autoridade autoridade, String mensagemNegacao) {
            if (temAutoridade(autoridade)) return true;
            String msg = (mensagemNegacao == null || mensagemNegacao.isBlank())
                    ? "Acesso negado: autoridade necessária (" + autoridade.name() + ")."
                    : mensagemNegacao;
            System.out.println(CoresAnsi.erro(msg + "\n"));
            return false;
        }

        // Verifica se o cargo vigente é OPERADOR
        boolean isOperador() {
            return "OPERADOR".equalsIgnoreCase(obterCargoVigente());
        }

        // Verifica se o usuário tem ao menos uma autoridade operacional/financeira/estoque/fornecedor
        boolean podeGerirOperacoes() {
            return temAutoridade(Autoridade.FIN_PAGAR_E_ESTORNAR) || temAutoridade(Autoridade.EST_GERIR_CATALOGO) || temAutoridade(Autoridade.FORN_GERIR_OPERACOES);
        }

        // Verifica se o usuário tem perfil de GERENTE ou permissão de auditoria/backup
        boolean isGerente() {
            return "GERENTE".equalsIgnoreCase(obterCargoVigente()) || temAutoridade(Autoridade.SIS_AUDITORIA_E_BACKUP);
        }

        // Exige permissão operacional básica
        boolean exigirPermissaoOperacional() {
            if (podeGerirOperacoes()) return true;
            System.out.println(CoresAnsi.erro(" Acesso negado para o seu perfil.\n"));
            return false;
        }

        // Exige permissão de gerente
        boolean exigirPermissaoGerente() {
            if (isGerente()) return true;
            System.out.println(CoresAnsi.erro(" Operação não permitida para o seu perfil.\n"));
            return false;
        }
    }

    // Controla o saldo em caixa global e os lançamentos financeiros/de dados
    static class ModuloCaixa {
        private final List<Double> saldoGlobal;
        private final List<String> tipos;
        private final List<String> envolvidos;
        private final List<String> descricoes;
        private final List<Double> valores;
        private final ModuloLogDadosTxt logDados;

        // Construtor que conecta o caixa às listas de transações e ao log em .TXT
        ModuloCaixa(List<Double> saldoGlobal,
                    List<String> tipos,
                    List<String> envolvidos,
                    List<String> descricoes,
                    List<Double> valores,
                    ModuloLogDadosTxt logDados) {
            this.saldoGlobal = saldoGlobal;
            this.tipos = tipos;
            this.envolvidos = envolvidos;
            this.descricoes = descricoes;
            this.valores = valores;
            this.logDados = logDados;
        }

        // Retorna o saldo atual do caixa com segurança contra valores inválidos
        double obterSaldo() {
            return (saldoGlobal != null && !saldoGlobal.isEmpty() && Double.isFinite(saldoGlobal.get(0)))
                    ? saldoGlobal.get(0)
                    : 0.0;
        }

        // Atualiza o saldo do caixa caso seja finito e não negativo
        void definirSaldo(double novoSaldo) {
            if (Double.isFinite(novoSaldo) && novoSaldo >= 0.0) {
                saldoGlobal.set(0, novoSaldo);
            }
        }

        // Registra movimentação puramente de dados (valor 0.0) no histórico e no log .TXT
        void registrarMovimentoDados(String direcao, String envolvidosStr, String descricao) {
            boolean ehEntrada = "ENTRADA_DADOS".equalsIgnoreCase(direcao) || "ENTRADA DE DADOS".equalsIgnoreCase(direcao);
            String tipoFormatado = ehEntrada ? "ENTRADA DE DADOS" : "SAÍDA DE DADOS";
            String envLimpo = sanitizarCampoTxt(envolvidosStr);
            String descLimpa = sanitizarCampoTxt(descricao);

            tipos.add(tipoFormatado);
            envolvidos.add(envLimpo);
            descricoes.add(descLimpa);
            valores.add(0.0);

            if (logDados != null) {
                logDados.gravarEvento(tipoFormatado, envLimpo, descLimpa);
            }
        }

        // Credita um valor positivo no caixa e registra a transação/log
        boolean registrarEntrada(double valor, String tipo, String envolvidosStr, String descricao) {
            if (!Double.isFinite(valor) || valor <= 0.0) {
                System.out.println(CoresAnsi.erro("Valor de entrada inválido.\n"));
                return false;
            }
            // Soma o valor ao saldo atual
            saldoGlobal.set(0, obterSaldo() + valor);
            String tipoLimpo = sanitizarCampoTxt(tipo);
            String envLimpo = sanitizarCampoTxt(envolvidosStr);
            String descLimpo = sanitizarCampoTxt(descricao);

            tipos.add(tipoLimpo);
            envolvidos.add(envLimpo);
            descricoes.add(descLimpo);
            valores.add(valor);

            if (logDados != null) {
                logDados.gravarEvento("ENTRADA DE DADOS (" + tipoLimpo + ")", envLimpo, descLimpo + " | Valor: " + formatarValorMonetarioReais(valor));
            }
            return true;
        }

        // Debita um valor positivo do caixa se houver saldo suficiente
        boolean registrarSaida(double valor, String tipo, String envolvidosStr, String descricao) {
            if (!Double.isFinite(valor) || valor <= 0.0) {
                System.out.println(CoresAnsi.erro("Valor de saída inválido.\n"));
                return false;
            }
            double saldoAtual = obterSaldo();
            if (saldoAtual < valor) {
                System.out.println(CoresAnsi.erro("Saldo insuficiente para concluir a operação.\n"));
                return false;
            }
            // Subtrai o valor do saldo atual
            saldoGlobal.set(0, saldoAtual - valor);
            String tipoLimpo = sanitizarCampoTxt(tipo);
            String envLimpo = sanitizarCampoTxt(envolvidosStr);
            String descLimpa = sanitizarCampoTxt(descricao);

            tipos.add(tipoLimpo);
            envolvidos.add(envLimpo);
            descricoes.add(descLimpa);
            valores.add(valor);

            if (logDados != null) {
                logDados.gravarEvento("SAÍDA DE DADOS (" + tipoLimpo + ")", envLimpo, descLimpa + " | Valor: " + formatarValorMonetarioReais(valor));
            }
            return true;
        }

        // Estorna um lançamento existente, ajustando o saldo conforme o tipo da operação
        boolean estornarLancamento(int indice) {
            if (indice < 0 || indice >= tipos.size()) {
                System.out.println(CoresAnsi.erro("Lançamento inexistente.\n"));
                return false;
            }

            String tipoLancamento = tipos.get(indice);
            double valorEstorno = valores.get(indice);
            double saldoAtual = obterSaldo();

            if ("VENDA".equalsIgnoreCase(tipoLancamento)) {
                // Estornar venda exige saldo suficiente para devolver o valor
                if (saldoAtual < valorEstorno) {
                    System.out.println(CoresAnsi.erro("Estorno bloqueado: saldo atual ("
                            + formatarValorMonetarioReais(saldoAtual)
                            + ") inferior ao valor do lançamento ("
                            + formatarValorMonetarioReais(valorEstorno) + ").\n"));
                    return false;
                }
                saldoGlobal.set(0, saldoAtual - valorEstorno);
            } else if ("COMPRA / ABASTECIMENTO".equalsIgnoreCase(tipoLancamento)
                    || "PAGAMENTO".equalsIgnoreCase(tipoLancamento)) {
                // Estornar saída devolve o valor ao caixa
                saldoGlobal.set(0, saldoAtual + valorEstorno);
            }

            // Remove o lançamento das listas paralelas
            tipos.remove(indice);
            envolvidos.remove(indice);
            descricoes.remove(indice);
            valores.remove(indice);
            return true;
        }
    }

    // Gerencia leitura de pré-cadastros, backups editáveis em .TXT e rollback automático
    static class ModuloPersistenciaTxt {
        private final String arquivoPreCadastros;
        private final String arquivoBackupOficial;
        private final String arquivoRollbackAuto;

        // Dados mínimos de emergência caso o arquivo de pré-cadastros esteja sem funcionários
        private static final List<String> EMERGENCIA_FUNCIONARIOS_POR_FUNCAO = Arrays.asList(
            "001;GERENTE EMERGENCIA;52998224725;GERENTE;emerg123;5500.0;false;ATIVO;NENHUM",
            "002;OPERADOR EMERGENCIA;12345678909;OPERADOR;emerg456;2200.0;false;ATIVO;NENHUM",
            "003;FINANCEIRO EMERGENCIA;39053344705;FINANCEIRO;emerg789;4800.0;false;ATIVO;NENHUM"
        );

        // Fornecedor de emergência caso a seção esteja vazia
        private static final List<String> EMERGENCIA_FORNECEDOR = Arrays.asList(
            "FORNECEDOR EMERGENCIA LTDA;11222333000181;SP;ITEM EMERGENCIA;2500.00;40.0;false"
        );

        // Produto de emergência caso a seção esteja vazia
        private static final List<String> EMERGENCIA_PRODUTO = Arrays.asList(
            "ITEM EMERGENCIA;2106.90.30;50.00;20"
        );

        ModuloPersistenciaTxt(String arquivoPreCadastros, String arquivoBackupOficial, String arquivoRollbackAuto) {
            this.arquivoPreCadastros = arquivoPreCadastros;
            this.arquivoBackupOficial = arquivoBackupOficial;
            this.arquivoRollbackAuto = arquivoRollbackAuto;
        }

        // Retorna o nome do arquivo de backup editável
        String obterNomeArquivoBackup() {
            return arquivoBackupOficial;
        }

        // Cria o arquivo de pré-cadastros se não existir e carrega suas seções
        boolean conferirECarregarPreCadastrosExternos(
                List<String> tabelaFuncId, List<String> tabelaFuncNome, List<String> tabelaFuncCpf,
                List<String> tabelaFuncCargo, List<String> tabelaFuncSenha, List<Double> tabelaFuncSalario,
                List<Boolean> tabelaFuncPago, List<String> tabelaFuncStatus, List<String> tabelaFuncMotivoInativo,
                List<String> tabelaFornRazaoSocial, List<String> tabelaFornCnpj, List<String> tabelaFornUf,
                List<String> tabelaFornProdutoPrincipal, List<Double> tabelaFornValorFatura,
                List<Double> tabelaFornMargemLucroPct, List<Boolean> tabelaFornFaturaPaga,
                List<String> tabelaProdNome, List<String> tabelaProdNcm, List<Double> tabelaProdPrecoVenda,
                List<Integer> tabelaProdQtdEstoque, List<String> tabelaClienteCpf, List<String> tabelaClienteNome) {

            File arquivo = new File(arquivoPreCadastros);
            if (!arquivo.exists()) {
                criarArquivoPreCadastrosPadraoSeAusente(arquivo);
            }

            try {
                lerSecoesPreCadastroDeArquivo(
                        arquivo,
                        tabelaFuncId, tabelaFuncNome, tabelaFuncCpf,
                        tabelaFuncCargo, tabelaFuncSenha, tabelaFuncSalario,
                        tabelaFuncPago, tabelaFuncStatus, tabelaFuncMotivoInativo,
                        tabelaFornRazaoSocial, tabelaFornCnpj, tabelaFornUf,
                        tabelaFornProdutoPrincipal, tabelaFornValorFatura,
                        tabelaFornMargemLucroPct, tabelaFornFaturaPaga,
                        tabelaProdNome, tabelaProdNcm, tabelaProdPrecoVenda,
                        tabelaProdQtdEstoque, tabelaClienteCpf, tabelaClienteNome
                );
                return true;
            } catch (Exception e) {
                System.out.println(CoresAnsi.erro("Erro ao consultar '" + arquivoPreCadastros + "': " + e.getMessage() + "\n"));
                return false;
            }
        }

        // Gera um arquivo inicial de pré-cadastros com blocos delimitados por ';'
        private void criarArquivoPreCadastrosPadraoSeAusente(File arquivo) {
            List<String> funcionariosArquivo = new ArrayList<>(EMERGENCIA_FUNCIONARIOS_POR_FUNCAO);
            funcionariosArquivo.addAll(Arrays.asList(
                "004;MARCOS OLIVEIRA;71428793860;OPERADOR;op999;2200.0;true;DEMITIDO;Corte de custos",
                "005;JULIA MENDES;85351394037;GERENTE;ger888;5500.0;true;DEMITIDO;Pedido de demissão",
                "006;ROBERTO ALVES;26890517008;OPERADOR;op111;2200.0;false;ATIVO;NENHUM",
                "007;PATRICIA LIMA;94271653015;FINANCEIRO;fin222;4800.0;false;ATIVO;NENHUM",
                "008;LUCAS SANTIAGO;11144477735;OPERADOR;op333;2200.0;false;ATIVO;NENHUM"
            ));

            List<String> fornecedoresArquivo = new ArrayList<>(EMERGENCIA_FORNECEDOR);
            fornecedoresArquivo.addAll(Arrays.asList(
                "VITAFIT DISTRIBUIDORA;11444777000161;RJ;CREATINA;3200.00;35.0;false",
                "GLOBAL NUTRI;19131243000197;MG;BCAA 100G;2800.00;40.0;false",
                "FARMACOMERCIAL BRASIL;33000167000101;PR;VITAMINAS;1900.00;45.0;false",
                "BIOHEDGER;60701190000104;RS;GLUTAMINA;3100.00;35.0;false",
                "ATLAS SUPLEMENTOS;00000000000191;SC;BARRA PROTEICA;1500.00;40.0;false"
            ));

            List<String> produtosArquivo = new ArrayList<>(EMERGENCIA_PRODUTO);
            produtosArquivo.addAll(Arrays.asList(
                "WHEY PROTEIN CONCENTRADO;2106.90.30;129.90;45",
                "CREATINA MONOHIDRATADA;2106.90.90;89.90;60",
                "BCAA 2400MG;2106.90.30;54.90;50",
                "MULTIVITAMINICO COMPLETO;2106.90.90;42.00;75",
                "GLUTAMINA PURA 300G;2106.90.30;68.50;40",
                "BARRA PROTEICA CAIXA;2106.90.90;79.00;35",
                "OMEGA 3 ULTRA;1517.90.90;49.90;55",
                "PRE-WORKOUT EXTREME;2106.90.90;115.00;30"
            ));

            List<String> clientesArquivo = Arrays.asList(
                "11144477735;JOAO PEREIRA",
                "39053344705;MARIA ALMEIDA",
                "71428793860;CARLOS EDUARDO",
                "85351394037;BEATRIZ SOUZA",
                "26890517008;LUCAS MARTINS",
                "94271653015;JULIANA ROCHA"
            );

            try (BufferedWriter escritor = new BufferedWriter(new FileWriter(arquivo))) {
                escritor.write("================================================================================\n");
                escritor.write("# ARQUIVO EXCLUSIVO DE PRÉ-CADASTROS (USUÁRIOS, CLIENTES, PRODUTOS E FORNECEDORES)\n");
                escritor.write("# Separador de campos: ';' | Linhas iniciadas com '#' são comentários\n");
                escritor.write("================================================================================\n\n");

                escritor.write("# [1] FUNCIONARIOS -> ID;NOME;CPF;CARGO;SENHA;SALARIO;PAGO;STATUS;MOTIVO\n");
                escritor.write("[FUNCIONARIOS_INICIO]\n");
                for (String linhaFunc : funcionariosArquivo) {
                    escritor.write(linhaFunc + "\n");
                }
                escritor.write("[FUNCIONARIOS_FIM]\n\n");

                escritor.write("# [2] FORNECEDORES -> RAZAO_SOCIAL;CNPJ;UF;PRODUTO;VALOR_FATURA;MARGEM_PCT;PAGO\n");
                escritor.write("[FORNECEDORES_INICIO]\n");
                for (String linhaForn : fornecedoresArquivo) {
                    escritor.write(linhaForn + "\n");
                }
                escritor.write("[FORNECEDORES_FIM]\n\n");

                escritor.write("# [3] PRODUTOS -> NOME;NCM;PRECO_VENDA;QTD_ESTOQUE\n");
                escritor.write("[PRODUTOS_INICIO]\n");
                for (String linhaProd : produtosArquivo) {
                    escritor.write(linhaProd + "\n");
                }
                escritor.write("[PRODUTOS_FIM]\n\n");

                escritor.write("# [4] CLIENTES -> CPF;NOME\n");
                escritor.write("[CLIENTES_INICIO]\n");
                for (String linhaCliente : clientesArquivo) {
                    escritor.write(linhaCliente + "\n");
                }
                escritor.write("[CLIENTES_FIM]\n");
            } catch (IOException ignored) {
            }
        }

        // Faz o parse de todas as seções do arquivo de pré-cadastros e popula as listas
        private void lerSecoesPreCadastroDeArquivo(
                File arquivo,
                List<String> tabelaFuncId, List<String> tabelaFuncNome, List<String> tabelaFuncCpf,
                List<String> tabelaFuncCargo, List<String> tabelaFuncSenha, List<Double> tabelaFuncSalario,
                List<Boolean> tabelaFuncPago, List<String> tabelaFuncStatus, List<String> tabelaFuncMotivoInativo,
                List<String> tabelaFornRazaoSocial, List<String> tabelaFornCnpj, List<String> tabelaFornUf,
                List<String> tabelaFornProdutoPrincipal, List<Double> tabelaFornValorFatura,
                List<Double> tabelaFornMargemLucroPct, List<Boolean> tabelaFornFaturaPaga,
                List<String> tabelaProdNome, List<String> tabelaProdNcm, List<Double> tabelaProdPrecoVenda,
                List<Integer> tabelaProdQtdEstoque, List<String> tabelaClienteCpf, List<String> tabelaClienteNome) throws Exception {

            // Listas temporárias para garantir atomicidade se ocorrer erro de leitura
            List<String> tempFuncId = new ArrayList<>(), tempFuncNome = new ArrayList<>(), tempFuncCpf = new ArrayList<>();
            List<String> tempFuncCargo = new ArrayList<>(), tempFuncSenha = new ArrayList<>(), tempFuncStatus = new ArrayList<>(), tempFuncMotivo = new ArrayList<>();
            List<Double> tempFuncSalario = new ArrayList<>();
            List<Boolean> tempFuncPago = new ArrayList<>();

            List<String> tempFornRazao = new ArrayList<>(), tempFornCnpj = new ArrayList<>(), tempFornUf = new ArrayList<>(), tempFornProd = new ArrayList<>();
            List<Double> tempFornFatura = new ArrayList<>(), tempFornMargem = new ArrayList<>();
            List<Boolean> tempFornPago = new ArrayList<>();

            List<String> tempProdNome = new ArrayList<>(), tempProdNcm = new ArrayList<>();
            List<Double> tempProdPreco = new ArrayList<>();
            List<Integer> tempProdEstoque = new ArrayList<>();

            List<String> tempClienteCpf = new ArrayList<>(), tempClienteNome = new ArrayList<>();

            try (BufferedReader leitor = new BufferedReader(new FileReader(arquivo))) {
                String linhaAtual, bloco = "";
                while ((linhaAtual = leitor.readLine()) != null) {
                    linhaAtual = linhaAtual.trim();
                    // Ignora linhas vazias, comentários e separadores visuais
                    if (linhaAtual.isEmpty() || linhaAtual.startsWith("#") || linhaAtual.startsWith("=")) continue;

                    // Identifica abertura e fechamento de cada bloco do arquivo
                    if (linhaAtual.equals("[FUNCIONARIOS_INICIO]")) { bloco = "FUNC"; continue; }
                    if (linhaAtual.equals("[FUNCIONARIOS_FIM]")) { bloco = ""; continue; }
                    if (linhaAtual.equals("[FORNECEDORES_INICIO]")) { bloco = "FORN"; continue; }
                    if (linhaAtual.equals("[FORNECEDORES_FIM]")) { bloco = ""; continue; }
                    if (linhaAtual.equals("[PRODUTOS_INICIO]")) { bloco = "PROD"; continue; }
                    if (linhaAtual.equals("[PRODUTOS_FIM]")) { bloco = ""; continue; }
                    if (linhaAtual.equals("[CLIENTES_INICIO]")) { bloco = "CLIENTE"; continue; }
                    if (linhaAtual.equals("[CLIENTES_FIM]")) { bloco = ""; continue; }

                    if (bloco.equals("FUNC")) {
                        String[] colunas = linhaAtual.split(";");
                        if (colunas.length < 9) throw new IllegalArgumentException("Linha inválida em FUNCIONARIOS: " + linhaAtual);
                        double salario = Double.parseDouble(colunas[5].trim().replace(",", "."));
                        if (!Double.isFinite(salario) || salario < 0) {
                            throw new IllegalArgumentException("Salário inválido em FUNCIONARIOS.");
                        }
                        tempFuncId.add(colunas[0].trim().toUpperCase());
                        tempFuncNome.add(colunas[1].trim().toUpperCase());
                        tempFuncCpf.add(colunas[2].trim().replaceAll("[^0-9]", ""));
                        tempFuncCargo.add(colunas[3].trim().toUpperCase());
                        tempFuncSenha.add(colunas[4].trim());
                        tempFuncSalario.add(salario);
                        tempFuncPago.add(Boolean.parseBoolean(colunas[6].trim()));
                        tempFuncStatus.add(colunas[7].trim().toUpperCase());
                        tempFuncMotivo.add(colunas[8].trim());
                    } else if (bloco.equals("FORN")) {
                        String[] colunas = linhaAtual.split(";");
                        if (colunas.length < 7) throw new IllegalArgumentException("Linha inválida em FORNECEDORES: " + linhaAtual);
                        double fatura = Double.parseDouble(colunas[4].trim().replace(",", "."));
                        double margem = Double.parseDouble(colunas[5].trim().replace(",", "."));
                        if (!Double.isFinite(fatura) || fatura < 0 || !Double.isFinite(margem) || margem < 0) {
                            throw new IllegalArgumentException("Valores de fornecedor inválidos.");
                        }
                        tempFornRazao.add(colunas[0].trim().toUpperCase());
                        tempFornCnpj.add(colunas[1].trim().replaceAll("[^0-9]", ""));
                        tempFornUf.add(colunas[2].trim().toUpperCase());
                        tempFornProd.add(colunas[3].trim().toUpperCase());
                        tempFornFatura.add(fatura);
                        tempFornMargem.add(margem);
                        tempFornPago.add(Boolean.parseBoolean(colunas[6].trim()));
                    } else if (bloco.equals("PROD")) {
                        String[] colunas = linhaAtual.split(";");
                        if (colunas.length < 4) throw new IllegalArgumentException("Linha inválida em PRODUTOS: " + linhaAtual);
                        double preco = Double.parseDouble(colunas[2].trim().replace(",", "."));
                        int estoque = Integer.parseInt(colunas[3].trim());
                        if (!Double.isFinite(preco) || preco <= 0 || estoque < 0) {
                            throw new IllegalArgumentException("Preço ou estoque inválido.");
                        }
                        tempProdNome.add(colunas[0].trim().toUpperCase());
                        tempProdNcm.add(colunas[1].trim());
                        tempProdPreco.add(preco);
                        tempProdEstoque.add(estoque);
                    } else if (bloco.equals("CLIENTE")) {
                        String[] colunas = linhaAtual.split(";");
                        if (colunas.length < 2) throw new IllegalArgumentException("Linha inválida em CLIENTES: " + linhaAtual);
                        tempClienteCpf.add(colunas[0].trim().replaceAll("[^0-9]", ""));
                        tempClienteNome.add(colunas[1].trim().toUpperCase());
                    }
                }
            }

            // Garante registros mínimos caso o arquivo tenha sido esvaziado manualmente
            if (tempFuncId.isEmpty()) {
                for (String linhaFunc : EMERGENCIA_FUNCIONARIOS_POR_FUNCAO) {
                    String[] colunas = linhaFunc.split(";");
                    tempFuncId.add(colunas[0].trim().toUpperCase());
                    tempFuncNome.add(colunas[1].trim().toUpperCase());
                    tempFuncCpf.add(colunas[2].trim());
                    tempFuncCargo.add(colunas[3].trim().toUpperCase());
                    tempFuncSenha.add(colunas[4].trim());
                    tempFuncSalario.add(Double.parseDouble(colunas[5].trim()));
                    tempFuncPago.add(Boolean.parseBoolean(colunas[6].trim()));
                    tempFuncStatus.add(colunas[7].trim().toUpperCase());
                    tempFuncMotivo.add(colunas[8].trim());
                }
            }

            if (tempFornRazao.isEmpty()) {
                for (String linhaForn : EMERGENCIA_FORNECEDOR) {
                    String[] colunas = linhaForn.split(";");
                    tempFornRazao.add(colunas[0].trim().toUpperCase());
                    tempFornCnpj.add(colunas[1].trim());
                    tempFornUf.add(colunas[2].trim().toUpperCase());
                    tempFornProd.add(colunas[3].trim().toUpperCase());
                    tempFornFatura.add(Double.parseDouble(colunas[4].trim()));
                    tempFornMargem.add(Double.parseDouble(colunas[5].trim()));
                    tempFornPago.add(Boolean.parseBoolean(colunas[6].trim()));
                }
            }

            if (tempProdNome.isEmpty()) {
                for (String linhaProd : EMERGENCIA_PRODUTO) {
                    String[] colunas = linhaProd.split(";");
                    tempProdNome.add(colunas[0].trim().toUpperCase());
                    tempProdNcm.add(colunas[1].trim());
                    tempProdPreco.add(Double.parseDouble(colunas[2].trim()));
                    tempProdEstoque.add(Integer.parseInt(colunas[3].trim()));
                }
            }

            // Substitui o conteúdo das tabelas oficiais pelos dados validados
            tabelaFuncId.clear(); tabelaFuncId.addAll(tempFuncId);
            tabelaFuncNome.clear(); tabelaFuncNome.addAll(tempFuncNome);
            tabelaFuncCpf.clear(); tabelaFuncCpf.addAll(tempFuncCpf);
            tabelaFuncCargo.clear(); tabelaFuncCargo.addAll(tempFuncCargo);
            tabelaFuncSenha.clear(); tabelaFuncSenha.addAll(tempFuncSenha);
            tabelaFuncSalario.clear(); tabelaFuncSalario.addAll(tempFuncSalario);
            tabelaFuncPago.clear(); tabelaFuncPago.addAll(tempFuncPago);
            tabelaFuncStatus.clear(); tabelaFuncStatus.addAll(tempFuncStatus);
            tabelaFuncMotivoInativo.clear(); tabelaFuncMotivoInativo.addAll(tempFuncMotivo);

            tabelaFornRazaoSocial.clear(); tabelaFornRazaoSocial.addAll(tempFornRazao);
            tabelaFornCnpj.clear(); tabelaFornCnpj.addAll(tempFornCnpj);
            tabelaFornUf.clear(); tabelaFornUf.addAll(tempFornUf);
            tabelaFornProdutoPrincipal.clear(); tabelaFornProdutoPrincipal.addAll(tempFornProd);
            tabelaFornValorFatura.clear(); tabelaFornValorFatura.addAll(tempFornFatura);
            tabelaFornMargemLucroPct.clear(); tabelaFornMargemLucroPct.addAll(tempFornMargem);
            tabelaFornFaturaPaga.clear(); tabelaFornFaturaPaga.addAll(tempFornPago);

            tabelaProdNome.clear(); tabelaProdNome.addAll(tempProdNome);
            tabelaProdNcm.clear(); tabelaProdNcm.addAll(tempProdNcm);
            tabelaProdPrecoVenda.clear(); tabelaProdPrecoVenda.addAll(tempProdPreco);
            tabelaProdQtdEstoque.clear(); tabelaProdQtdEstoque.addAll(tempProdEstoque);

            tabelaClienteCpf.clear(); tabelaClienteCpf.addAll(tempClienteCpf);
            tabelaClienteNome.clear(); tabelaClienteNome.addAll(tempClienteNome);
        }

        // Grava todo o estado atual do sistema (incluindo autoridades, transações e caixa) em um arquivo .TXT
        private boolean gravarEmArquivo(
                String caminhoArquivo, String cabecalho,
                ModuloAutoridades moduloAutoridades,
                List<String> tabelaFuncId, List<String> tabelaFuncNome, List<String> tabelaFuncCpf,
                List<String> tabelaFuncCargo, List<String> tabelaFuncSenha, List<Double> tabelaFuncSalario,
                List<Boolean> tabelaFuncPago, List<String> tabelaFuncStatus, List<String> tabelaFuncMotivoInativo,
                List<String> tabelaFornRazaoSocial, List<String> tabelaFornCnpj, List<String> tabelaFornUf,
                List<String> tabelaFornProdutoPrincipal, List<Double> tabelaFornValorFatura,
                List<Double> tabelaFornMargemLucroPct, List<Boolean> tabelaFornFaturaPaga,
                List<String> tabelaProdNome, List<String> tabelaProdNcm, List<Double> tabelaProdPrecoVenda,
                List<Integer> tabelaProdQtdEstoque, List<String> tabelaClienteCpf, List<String> tabelaClienteNome,
                List<String> tabelaTransacaoTipo, List<String> tabelaTransacaoEnvolvidos,
                List<String> tabelaTransacaoDescricao, List<Double> tabelaTransacaoValor,
                double saldoCaixaAtual) {

            try (BufferedWriter escritorArquivo = new BufferedWriter(new FileWriter(caminhoArquivo))) {
                escritorArquivo.write("================================================================================\n");
                escritorArquivo.write(cabecalho + "\n");
                escritorArquivo.write("# Dica: Edite este .TXT com o mesmo separador ';' e use Carregar arquivo .TXT.\n");
                escritorArquivo.write("================================================================================\n\n");

                // Bloco 1: Funcionários
                escritorArquivo.write("# [1] FUNCIONARIOS -> ID;NOME;CPF;CARGO;SENHA;SALARIO;PAGO;STATUS;MOTIVO\n");
                escritorArquivo.write("[FUNCIONARIOS_INICIO]\n");
                for (int i = 0; i < tabelaFuncId.size(); i++) {
                    escritorArquivo.write(
                            sanitizarCampoTxt(tabelaFuncId.get(i)) + ";" +
                            sanitizarCampoTxt(tabelaFuncNome.get(i)) + ";" +
                            sanitizarCampoTxt(tabelaFuncCpf.get(i)) + ";" +
                            sanitizarCampoTxt(tabelaFuncCargo.get(i)) + ";" +
                            sanitizarCampoTxt(tabelaFuncSenha.get(i)) + ";" +
                            tabelaFuncSalario.get(i) + ";" +
                            tabelaFuncPago.get(i) + ";" +
                            sanitizarCampoTxt(tabelaFuncStatus.get(i)) + ";" +
                            sanitizarCampoTxt(tabelaFuncMotivoInativo.get(i)) + "\n"
                    );
                }
                escritorArquivo.write("[FUNCIONARIOS_FIM]\n\n");

                // Bloco 1B: Autoridades personalizadas por ID de funcionário
                if (moduloAutoridades != null) {
                    escritorArquivo.write("# [1B] AUTORIDADES -> ID_FUNCIONARIO;AUT1,AUT2,...\n");
                    escritorArquivo.write("[AUTORIDADES_INICIO]\n");
                    for (int i = 0; i < tabelaFuncId.size(); i++) {
                        String idFunc = tabelaFuncId.get(i);
                        boolean ativo = i < tabelaFuncStatus.size() && "ATIVO".equalsIgnoreCase(tabelaFuncStatus.get(i));
                        EnumSet<Autoridade> auts = moduloAutoridades.obterAutoridades(idFunc, ativo);
                        List<String> nomes = new ArrayList<>();
                        for (Autoridade a : auts) nomes.add(a.name());
                        escritorArquivo.write(sanitizarCampoTxt(idFunc) + ";" + String.join(",", nomes) + "\n");
                    }
                    escritorArquivo.write("[AUTORIDADES_FIM]\n\n");
                }

                // Bloco 2: Fornecedores
                escritorArquivo.write("# [2] FORNECEDORES -> RAZAO_SOCIAL;CNPJ;UF;PRODUTO;VALOR_FATURA;MARGEM_PCT;PAGO\n");
                escritorArquivo.write("[FORNECEDORES_INICIO]\n");
                for (int i = 0; i < tabelaFornRazaoSocial.size(); i++) {
                    escritorArquivo.write(
                            sanitizarCampoTxt(tabelaFornRazaoSocial.get(i)) + ";" +
                            sanitizarCampoTxt(tabelaFornCnpj.get(i)) + ";" +
                            sanitizarCampoTxt(tabelaFornUf.get(i)) + ";" +
                            sanitizarCampoTxt(tabelaFornProdutoPrincipal.get(i)) + ";" +
                            tabelaFornValorFatura.get(i) + ";" +
                            tabelaFornMargemLucroPct.get(i) + ";" +
                            tabelaFornFaturaPaga.get(i) + "\n"
                    );
                }
                escritorArquivo.write("[FORNECEDORES_FIM]\n\n");

                // Bloco 3: Produtos
                escritorArquivo.write("# [3] PRODUTOS -> NOME;NCM;PRECO_VENDA;QTD_ESTOQUE\n");
                escritorArquivo.write("[PRODUTOS_INICIO]\n");
                for (int i = 0; i < tabelaProdNome.size(); i++) {
                    if (tabelaProdNome.get(i) != null) {
                        escritorArquivo.write(
                                sanitizarCampoTxt(tabelaProdNome.get(i)) + ";" +
                                sanitizarCampoTxt(tabelaProdNcm.get(i)) + ";" +
                                tabelaProdPrecoVenda.get(i) + ";" +
                                tabelaProdQtdEstoque.get(i) + "\n"
                        );
                    }
                }
                escritorArquivo.write("[PRODUTOS_FIM]\n\n");

                // Bloco 4: Clientes
                escritorArquivo.write("# [4] CLIENTES -> CPF;NOME\n");
                escritorArquivo.write("[CLIENTES_INICIO]\n");
                for (int i = 0; i < tabelaClienteCpf.size(); i++) {
                    escritorArquivo.write(
                            sanitizarCampoTxt(tabelaClienteCpf.get(i)) + ";" +
                            sanitizarCampoTxt(tabelaClienteNome.get(i)) + "\n"
                    );
                }
                escritorArquivo.write("[CLIENTES_FIM]\n\n");

                // Bloco 5: Transações financeiras e de dados
                escritorArquivo.write("# [5] TRANSACOES (FINANCEIRO & ENTRADAS/SAÍDAS) -> TIPO;ENVOLVIDOS;DESCRICAO;VALOR\n");
                escritorArquivo.write("[TRANSACOES_INICIO]\n");
                for (int i = 0; i < tabelaTransacaoTipo.size(); i++) {
                    escritorArquivo.write(
                            sanitizarCampoTxt(tabelaTransacaoTipo.get(i)) + ";" +
                            sanitizarCampoTxt(tabelaTransacaoEnvolvidos.get(i)) + ";" +
                            sanitizarCampoTxt(tabelaTransacaoDescricao.get(i)) + ";" +
                            tabelaTransacaoValor.get(i) + "\n"
                    );
                }
                escritorArquivo.write("[TRANSACOES_FIM]\n\n");

                // Bloco 6: Saldo global em caixa
                escritorArquivo.write("# [6] SALDO EM CAIXA GLOBAL (número >= 0)\n");
                escritorArquivo.write("[CAIXA_INICIO]\n" + saldoCaixaAtual + "\n[CAIXA_FIM]\n");
                return true;
            } catch (IOException e) {
                return false;
            }
        }

        // Atalho para gravar o arquivo de backup oficial editável
        boolean salvarBackupEditavel(
                ModuloAutoridades moduloAutoridades,
                List<String> tabelaFuncId, List<String> tabelaFuncNome, List<String> tabelaFuncCpf,
                List<String> tabelaFuncCargo, List<String> tabelaFuncSenha, List<Double> tabelaFuncSalario,
                List<Boolean> tabelaFuncPago, List<String> tabelaFuncStatus, List<String> tabelaFuncMotivoInativo,
                List<String> tabelaFornRazaoSocial, List<String> tabelaFornCnpj, List<String> tabelaFornUf,
                List<String> tabelaFornProdutoPrincipal, List<Double> tabelaFornValorFatura,
                List<Double> tabelaFornMargemLucroPct, List<Boolean> tabelaFornFaturaPaga,
                List<String> tabelaProdNome, List<String> tabelaProdNcm, List<Double> tabelaProdPrecoVenda,
                List<Integer> tabelaProdQtdEstoque, List<String> tabelaClienteCpf, List<String> tabelaClienteNome,
                List<String> tabelaTransacaoTipo, List<String> tabelaTransacaoEnvolvidos,
                List<String> tabelaTransacaoDescricao, List<Double> tabelaTransacaoValor,
                ModuloCaixa caixa) {

            return gravarEmArquivo(
                    arquivoBackupOficial,
                    "# BACKUP EDITÁVEL PROTEI CREAT (L'ESTOQUE)",
                    moduloAutoridades,
                    tabelaFuncId, tabelaFuncNome, tabelaFuncCpf, tabelaFuncCargo, tabelaFuncSenha,
                    tabelaFuncSalario, tabelaFuncPago, tabelaFuncStatus, tabelaFuncMotivoInativo,
                    tabelaFornRazaoSocial, tabelaFornCnpj, tabelaFornUf, tabelaFornProdutoPrincipal,
                    tabelaFornValorFatura, tabelaFornMargemLucroPct, tabelaFornFaturaPaga,
                    tabelaProdNome, tabelaProdNcm, tabelaProdPrecoVenda, tabelaProdQtdEstoque,
                    tabelaClienteCpf, tabelaClienteNome,
                    tabelaTransacaoTipo, tabelaTransacaoEnvolvidos, tabelaTransacaoDescricao, tabelaTransacaoValor,
                    caixa.obterSaldo()
            );
        }

        // Salva um snapshot automático em arquivo antes de restaurar um backup ou cadastrar funcionário/item
        void executarRollbackAutomatico(
                ModuloAutoridades moduloAutoridades,
                List<String> tabelaFuncId, List<String> tabelaFuncNome, List<String> tabelaFuncCpf,
                List<String> tabelaFuncCargo, List<String> tabelaFuncSenha, List<Double> tabelaFuncSalario,
                List<Boolean> tabelaFuncPago, List<String> tabelaFuncStatus, List<String> tabelaFuncMotivoInativo,
                List<String> tabelaFornRazaoSocial, List<String> tabelaFornCnpj, List<String> tabelaFornUf,
                List<String> tabelaFornProdutoPrincipal, List<Double> tabelaFornValorFatura,
                List<Double> tabelaFornMargemLucroPct, List<Boolean> tabelaFornFaturaPaga,
                List<String> tabelaProdNome, List<String> tabelaProdNcm, List<Double> tabelaProdPrecoVenda,
                List<Integer> tabelaProdQtdEstoque, List<String> tabelaClienteCpf, List<String> tabelaClienteNome,
                List<String> tabelaTransacaoTipo, List<String> tabelaTransacaoEnvolvidos,
                List<String> tabelaTransacaoDescricao, List<Double> tabelaTransacaoValor,
                ModuloCaixa caixa) {

            gravarEmArquivo(
                    arquivoRollbackAuto,
                    "# PONTO DE RESTAURAÇÃO AUTOMÁTICO (ROLLBACK)",
                    moduloAutoridades,
                    tabelaFuncId, tabelaFuncNome, tabelaFuncCpf, tabelaFuncCargo, tabelaFuncSenha,
                    tabelaFuncSalario, tabelaFuncPago, tabelaFuncStatus, tabelaFuncMotivoInativo,
                    tabelaFornRazaoSocial, tabelaFornCnpj, tabelaFornUf, tabelaFornProdutoPrincipal,
                    tabelaFornValorFatura, tabelaFornMargemLucroPct, tabelaFornFaturaPaga,
                    tabelaProdNome, tabelaProdNcm, tabelaProdPrecoVenda, tabelaProdQtdEstoque,
                    tabelaClienteCpf, tabelaClienteNome,
                    tabelaTransacaoTipo, tabelaTransacaoEnvolvidos, tabelaTransacaoDescricao, tabelaTransacaoValor,
                    caixa.obterSaldo()
            );
        }

        // Exige confirmação de senha do usuário autenticado antes de exportar o backup .TXT
        boolean exportarComConfirmacao(
                Scanner leitorEntrada, ModuloSessao sessao, ModuloAuditoria auditoria, ModuloAutoridades moduloAutoridades,
                List<String> tabelaFuncId, List<String> tabelaFuncNome, List<String> tabelaFuncCpf,
                List<String> tabelaFuncCargo, List<String> tabelaFuncSenha, List<Double> tabelaFuncSalario,
                List<Boolean> tabelaFuncPago, List<String> tabelaFuncStatus, List<String> tabelaFuncMotivoInativo,
                List<String> tabelaFornRazaoSocial, List<String> tabelaFornCnpj, List<String> tabelaFornUf,
                List<String> tabelaFornProdutoPrincipal, List<Double> tabelaFornValorFatura,
                List<Double> tabelaFornMargemLucroPct, List<Boolean> tabelaFornFaturaPaga,
                List<String> tabelaProdNome, List<String> tabelaProdNcm, List<Double> tabelaProdPrecoVenda,
                List<Integer> tabelaProdQtdEstoque, List<String> tabelaClienteCpf, List<String> tabelaClienteNome,
                List<String> tabelaTransacaoTipo, List<String> tabelaTransacaoEnvolvidos,
                List<String> tabelaTransacaoDescricao, List<Double> tabelaTransacaoValor,
                ModuloCaixa caixa) {

            if (!sessao.exigirAutoridade(Autoridade.SIS_AUDITORIA_E_BACKUP, "Sem autoridade para exportar backup.")) {
                return false;
            }

            System.out.println(CoresAnsi.titulo("\n================================================================"));
            System.out.println(CoresAnsi.titulo("               EXPORTAR / ATUALIZAR ARQUIVO .TXT                "));
            System.out.println(CoresAnsi.titulo("================================================================"));
            System.out.printf("Resumo Atual -> Cadastros: %d | Fornecimento: %d | Itens: %d | Clientes: %d | Lançamentos: %d\n",
                    tabelaFuncId.size(), tabelaFornRazaoSocial.size(), tabelaProdNome.size(), tabelaClienteCpf.size(), tabelaTransacaoTipo.size());

            // Solicita a senha atual para confirmar a operação sensível
            System.out.print(" Confirme sua senha [0 para cancelar]: ");
            String senhaConfirmacao = lerLinhaSegura(leitorEntrada);
            if (isComandoCancelamento(senhaConfirmacao) || !senhaConfirmacao.equals(sessao.obterSenhaVigente())) {
                System.out.println(CoresAnsi.erro("Senha incorreta ou operação cancelada.\n"));
                return false;
            }

            caixa.registrarMovimentoDados(
                    "SAIDA_DADOS",
                    "Operador: " + sessao.obterNomeVigente(),
                    "Exportação de backup para '" + arquivoBackupOficial + "' (" + tabelaTransacaoTipo.size() + " lançamentos)"
            );

            boolean gravou = salvarBackupEditavel(
                    moduloAutoridades,
                    tabelaFuncId, tabelaFuncNome, tabelaFuncCpf, tabelaFuncCargo, tabelaFuncSenha,
                    tabelaFuncSalario, tabelaFuncPago, tabelaFuncStatus, tabelaFuncMotivoInativo,
                    tabelaFornRazaoSocial, tabelaFornCnpj, tabelaFornUf, tabelaFornProdutoPrincipal,
                    tabelaFornValorFatura, tabelaFornMargemLucroPct, tabelaFornFaturaPaga,
                    tabelaProdNome, tabelaProdNcm, tabelaProdPrecoVenda, tabelaProdQtdEstoque,
                    tabelaClienteCpf, tabelaClienteNome,
                    tabelaTransacaoTipo, tabelaTransacaoEnvolvidos, tabelaTransacaoDescricao, tabelaTransacaoValor,
                    caixa
            );

            if (gravou) {
                auditoria.registrar(sessao.obterNomeVigente(), "Exportou/atualizou arquivo editável " + arquivoBackupOficial);
                System.out.println(CoresAnsi.sucesso(" Arquivo '" + arquivoBackupOficial + "' salvo com sucesso.\n"));
            } else {
                System.out.println(CoresAnsi.erro("Falha técnica ao gravar arquivo.\n"));
            }
            return gravou;
        }

        // Gera rollback automático e carrega os dados do arquivo de backup oficial após confirmar senha
        boolean restaurarBackupOficial(
                Scanner leitorEntrada, ModuloSessao sessao, ModuloAuditoria auditoria,
                ModuloAutoridades moduloAutoridades,
                List<String> tabelaFuncId, List<String> tabelaFuncNome, List<String> tabelaFuncCpf,
                List<String> tabelaFuncCargo, List<String> tabelaFuncSenha, List<Double> tabelaFuncSalario,
                List<Boolean> tabelaFuncPago, List<String> tabelaFuncStatus, List<String> tabelaFuncMotivoInativo,
                List<String> tabelaFornRazaoSocial, List<String> tabelaFornCnpj, List<String> tabelaFornUf,
                List<String> tabelaFornProdutoPrincipal, List<Double> tabelaFornValorFatura,
                List<Double> tabelaFornMargemLucroPct, List<Boolean> tabelaFornFaturaPaga,
                List<String> tabelaProdNome, List<String> tabelaProdNcm, List<Double> tabelaProdPrecoVenda,
                List<Integer> tabelaProdQtdEstoque, List<String> tabelaClienteCpf, List<String> tabelaClienteNome,
                List<String> tabelaTransacaoTipo, List<String> tabelaTransacaoEnvolvidos,
                List<String> tabelaTransacaoDescricao, List<Double> tabelaTransacaoValor,
                ModuloCaixa caixa) {

            if (!sessao.exigirAutoridade(Autoridade.SIS_AUDITORIA_E_BACKUP, "Sem autoridade para restaurar backup.")) {
                return false;
            }

            System.out.print(" Confirme sua senha [0 para cancelar]: ");
            String senhaConfirmacao = lerLinhaSegura(leitorEntrada);
            if (isComandoCancelamento(senhaConfirmacao) || !senhaConfirmacao.equals(sessao.obterSenhaVigente())) {
                System.out.println(CoresAnsi.erro("Senha incorreta ou operação cancelada.\n"));
                return false;
            }

            File arquivoBackup = new File(arquivoBackupOficial);
            if (!arquivoBackup.exists()) {
                System.out.println(CoresAnsi.aviso(" Arquivo '" + arquivoBackupOficial + "' não encontrado.\n"));
                return false;
            }

            // Cria ponto de segurança antes de sobrescrever os dados em memória
            executarRollbackAutomatico(
                    moduloAutoridades,
                    tabelaFuncId, tabelaFuncNome, tabelaFuncCpf, tabelaFuncCargo, tabelaFuncSenha,
                    tabelaFuncSalario, tabelaFuncPago, tabelaFuncStatus, tabelaFuncMotivoInativo,
                    tabelaFornRazaoSocial, tabelaFornCnpj, tabelaFornUf, tabelaFornProdutoPrincipal,
                    tabelaFornValorFatura, tabelaFornMargemLucroPct, tabelaFornFaturaPaga,
                    tabelaProdNome, tabelaProdNcm, tabelaProdPrecoVenda, tabelaProdQtdEstoque,
                    tabelaClienteCpf, tabelaClienteNome,
                    tabelaTransacaoTipo, tabelaTransacaoEnvolvidos, tabelaTransacaoDescricao, tabelaTransacaoValor,
                    caixa
            );

            try {
                carregarDeArquivo(
                        arquivoBackup, moduloAutoridades,
                        tabelaFuncId, tabelaFuncNome, tabelaFuncCpf, tabelaFuncCargo, tabelaFuncSenha,
                        tabelaFuncSalario, tabelaFuncPago, tabelaFuncStatus, tabelaFuncMotivoInativo,
                        tabelaFornRazaoSocial, tabelaFornCnpj, tabelaFornUf, tabelaFornProdutoPrincipal,
                        tabelaFornValorFatura, tabelaFornMargemLucroPct, tabelaFornFaturaPaga,
                        tabelaProdNome, tabelaProdNcm, tabelaProdPrecoVenda, tabelaProdQtdEstoque,
                        tabelaClienteCpf, tabelaClienteNome,
                        tabelaTransacaoTipo, tabelaTransacaoEnvolvidos, tabelaTransacaoDescricao, tabelaTransacaoValor,
                        caixa
                );
                if (moduloAutoridades != null) {
                    moduloAutoridades.sincronizarComTabelaFuncionarios(tabelaFuncId, tabelaFuncCargo, tabelaFuncStatus);
                }
                caixa.registrarMovimentoDados(
                        "ENTRADA_DADOS",
                        "Operador: " + sessao.obterNomeVigente(),
                        "Restauração de dados a partir de '" + arquivoBackupOficial + "'"
                );
                auditoria.registrar(sessao.obterNomeVigente(), "Restaurou/aplicou alterações do arquivo " + arquivoBackupOficial);
                System.out.println(CoresAnsi.sucesso(" Dados atualizados a partir de '" + arquivoBackupOficial + "'.\n"));
                return true;
            } catch (Exception e) {
                System.out.println(CoresAnsi.erro("Erro ao ler/validar '" + arquivoBackupOficial + "': " + e.getMessage() + "\n"));
                return false;
            }
        }

        // Lê todos os blocos de um arquivo de backup (incluindo autoridades, transações e caixa)
        private void carregarDeArquivo(
                File arquivo, ModuloAutoridades moduloAutoridades,
                List<String> tabelaFuncId, List<String> tabelaFuncNome, List<String> tabelaFuncCpf,
                List<String> tabelaFuncCargo, List<String> tabelaFuncSenha, List<Double> tabelaFuncSalario,
                List<Boolean> tabelaFuncPago, List<String> tabelaFuncStatus, List<String> tabelaFuncMotivoInativo,
                List<String> tabelaFornRazaoSocial, List<String> tabelaFornCnpj, List<String> tabelaFornUf,
                List<String> tabelaFornProdutoPrincipal, List<Double> tabelaFornValorFatura,
                List<Double> tabelaFornMargemLucroPct, List<Boolean> tabelaFornFaturaPaga,
                List<String> tabelaProdNome, List<String> tabelaProdNcm, List<Double> tabelaProdPrecoVenda,
                List<Integer> tabelaProdQtdEstoque, List<String> tabelaClienteCpf, List<String> tabelaClienteNome,
                List<String> tabelaTransacaoTipo, List<String> tabelaTransacaoEnvolvidos,
                List<String> tabelaTransacaoDescricao, List<Double> tabelaTransacaoValor,
                ModuloCaixa caixa) throws Exception {

            List<String> tempFuncId = new ArrayList<>(), tempFuncNome = new ArrayList<>(), tempFuncCpf = new ArrayList<>();
            List<String> tempFuncCargo = new ArrayList<>(), tempFuncSenha = new ArrayList<>(), tempFuncStatus = new ArrayList<>(), tempFuncMotivo = new ArrayList<>();
            List<Double> tempFuncSalario = new ArrayList<>();
            List<Boolean> tempFuncPago = new ArrayList<>();

            // Guarda temporariamente as autoridades lidas por ID de funcionário
            Map<String, List<String>> tempAutoridadesPorId = new LinkedHashMap<>();

            List<String> tempFornRazao = new ArrayList<>(), tempFornCnpj = new ArrayList<>(), tempFornUf = new ArrayList<>(), tempFornProd = new ArrayList<>();
            List<Double> tempFornFatura = new ArrayList<>(), tempFornMargem = new ArrayList<>();
            List<Boolean> tempFornPago = new ArrayList<>();

            List<String> tempProdNome = new ArrayList<>(), tempProdNcm = new ArrayList<>();
            List<Double> tempProdPreco = new ArrayList<>();
            List<Integer> tempProdEstoque = new ArrayList<>();

            List<String> tempClienteCpf = new ArrayList<>(), tempClienteNome = new ArrayList<>();

            List<String> tempTransTipo = new ArrayList<>(), tempTransEnvolvidos = new ArrayList<>(), tempTransDesc = new ArrayList<>();
            List<Double> tempTransValor = new ArrayList<>();

            double tempCaixa = caixa.obterSaldo();

            try (BufferedReader leitorArquivo = new BufferedReader(new FileReader(arquivo))) {
                String linhaAtual, blocoLeitura = "";
                while ((linhaAtual = leitorArquivo.readLine()) != null) {
                    linhaAtual = linhaAtual.trim();
                    if (linhaAtual.isEmpty() || linhaAtual.startsWith("#") || linhaAtual.startsWith("=")) continue;

                    // Controle de estado do bloco atual do arquivo
                    if (linhaAtual.equals("[FUNCIONARIOS_INICIO]")) { blocoLeitura = "FUNC"; continue; }
                    if (linhaAtual.equals("[FUNCIONARIOS_FIM]")) { blocoLeitura = ""; continue; }
                    if (linhaAtual.equals("[AUTORIDADES_INICIO]")) { blocoLeitura = "AUT"; continue; }
                    if (linhaAtual.equals("[AUTORIDADES_FIM]")) { blocoLeitura = ""; continue; }
                    if (linhaAtual.equals("[FORNECEDORES_INICIO]")) { blocoLeitura = "FORN"; continue; }
                    if (linhaAtual.equals("[FORNECEDORES_FIM]")) { blocoLeitura = ""; continue; }
                    if (linhaAtual.equals("[PRODUTOS_INICIO]")) { blocoLeitura = "PROD"; continue; }
                    if (linhaAtual.equals("[PRODUTOS_FIM]")) { blocoLeitura = ""; continue; }
                    if (linhaAtual.equals("[CLIENTES_INICIO]")) { blocoLeitura = "CLIENTE"; continue; }
                    if (linhaAtual.equals("[CLIENTES_FIM]")) { blocoLeitura = ""; continue; }
                    if (linhaAtual.equals("[TRANSACOES_INICIO]")) { blocoLeitura = "TRANS"; continue; }
                    if (linhaAtual.equals("[TRANSACOES_FIM]")) { blocoLeitura = ""; continue; }
                    if (linhaAtual.equals("[CAIXA_INICIO]")) { blocoLeitura = "CAIXA"; continue; }
                    if (linhaAtual.equals("[CAIXA_FIM]")) { blocoLeitura = ""; continue; }

                    if (blocoLeitura.equals("FUNC")) {
                        String[] colunas = linhaAtual.split(";");
                        if (colunas.length < 9) throw new IllegalArgumentException("Linha inválida em FUNCIONARIOS: " + linhaAtual);
                        double salario = Double.parseDouble(colunas[5].trim().replace(",", "."));
                        if (!Double.isFinite(salario) || salario < 0) throw new IllegalArgumentException("Salário inválido no .TXT.");
                        tempFuncId.add(colunas[0].trim().toUpperCase());
                        tempFuncNome.add(colunas[1].trim().toUpperCase());
                        tempFuncCpf.add(colunas[2].trim().replaceAll("[^0-9]", ""));
                        tempFuncCargo.add(colunas[3].trim().toUpperCase());
                        tempFuncSenha.add(colunas[4].trim());
                        tempFuncSalario.add(salario);
                        tempFuncPago.add(Boolean.parseBoolean(colunas[6].trim()));
                        tempFuncStatus.add(colunas[7].trim().toUpperCase());
                        tempFuncMotivo.add(colunas[8].trim());
                    } else if (blocoLeitura.equals("AUT")) {
                        // Lê linha no formato ID_FUNCIONARIO;AUT1,AUT2,...
                        String[] colunas = linhaAtual.split(";");
                        if (colunas.length >= 1 && !colunas[0].trim().isEmpty()) {
                            String idAut = colunas[0].trim().toUpperCase();
                            List<String> listaAuts = new ArrayList<>();
                            if (colunas.length >= 2 && !colunas[1].trim().isEmpty()) {
                                for (String item : colunas[1].split(",")) {
                                    listaAuts.add(item.trim());
                                }
                            }
                            tempAutoridadesPorId.put(idAut, listaAuts);
                        }
                    } else if (blocoLeitura.equals("FORN")) {
                        String[] colunas = linhaAtual.split(";");
                        if (colunas.length < 7) throw new IllegalArgumentException("Linha inválida em FORNECEDORES: " + linhaAtual);
                        double fatura = Double.parseDouble(colunas[4].trim().replace(",", "."));
                        double margem = Double.parseDouble(colunas[5].trim().replace(",", "."));
                        if (!Double.isFinite(fatura) || fatura < 0 || !Double.isFinite(margem) || margem < 0) {
                            throw new IllegalArgumentException("Valores de fornecedor inválidos no .TXT.");
                        }
                        tempFornRazao.add(colunas[0].trim().toUpperCase());
                        tempFornCnpj.add(colunas[1].trim().replaceAll("[^0-9]", ""));
                        tempFornUf.add(colunas[2].trim().toUpperCase());
                        tempFornProd.add(colunas[3].trim().toUpperCase());
                        tempFornFatura.add(fatura);
                        tempFornMargem.add(margem);
                        tempFornPago.add(Boolean.parseBoolean(colunas[6].trim()));
                    } else if (blocoLeitura.equals("PROD")) {
                        String[] colunas = linhaAtual.split(";");
                        if (colunas.length < 4) throw new IllegalArgumentException("Linha inválida em PRODUTOS: " + linhaAtual);
                        double preco = Double.parseDouble(colunas[2].trim().replace(",", "."));
                        int estoque = Integer.parseInt(colunas[3].trim());
                        if (!Double.isFinite(preco) || preco <= 0 || estoque < 0) {
                            throw new IllegalArgumentException("Preço ou estoque inválido no .TXT.");
                        }
                        tempProdNome.add(colunas[0].trim().toUpperCase());
                        tempProdNcm.add(colunas[1].trim());
                        tempProdPreco.add(preco);
                        tempProdEstoque.add(estoque);
                    } else if (blocoLeitura.equals("CLIENTE")) {
                        String[] colunas = linhaAtual.split(";");
                        if (colunas.length < 2) throw new IllegalArgumentException("Linha inválida em CLIENTES: " + linhaAtual);
                        tempClienteCpf.add(colunas[0].trim().replaceAll("[^0-9]", ""));
                        tempClienteNome.add(colunas[1].trim().toUpperCase());
                    } else if (blocoLeitura.equals("TRANS")) {
                        String[] colunas = linhaAtual.split(";");
                        if (colunas.length < 4) throw new IllegalArgumentException("Linha inválida em TRANSACOES: " + linhaAtual);
                        double valorTrans = Double.parseDouble(colunas[3].trim().replace(",", "."));
                        if (!Double.isFinite(valorTrans) || valorTrans < 0.0) {
                            throw new IllegalArgumentException("Valor de transação inválido no .TXT.");
                        }
                        tempTransTipo.add(colunas[0].trim());
                        tempTransEnvolvidos.add(colunas[1].trim());
                        tempTransDesc.add(colunas[2].trim());
                        tempTransValor.add(valorTrans);
                    } else if (blocoLeitura.equals("CAIXA")) {
                        tempCaixa = Double.parseDouble(linhaAtual.replace(",", "."));
                        if (!Double.isFinite(tempCaixa) || tempCaixa < 0.0) {
                            throw new IllegalArgumentException("Saldo de caixa inválido no .TXT.");
                        }
                    }
                }
            }

            if (tempFuncId.isEmpty()) {
                for (String linhaFunc : EMERGENCIA_FUNCIONARIOS_POR_FUNCAO) {
                    String[] colunas = linhaFunc.split(";");
                    tempFuncId.add(colunas[0].trim().toUpperCase());
                    tempFuncNome.add(colunas[1].trim().toUpperCase());
                    tempFuncCpf.add(colunas[2].trim());
                    tempFuncCargo.add(colunas[3].trim().toUpperCase());
                    tempFuncSenha.add(colunas[4].trim());
                    tempFuncSalario.add(Double.parseDouble(colunas[5].trim()));
                    tempFuncPago.add(Boolean.parseBoolean(colunas[6].trim()));
                    tempFuncStatus.add(colunas[7].trim().toUpperCase());
                    tempFuncMotivo.add(colunas[8].trim());
                }
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

            // Aplica as autoridades carregadas para cada ID de funcionário ativo
            if (moduloAutoridades != null) {
                for (Map.Entry<String, List<String>> entry : tempAutoridadesPorId.entrySet()) {
                    String idFunc = entry.getKey();
                    int idx = tabelaFuncId.indexOf(idFunc);
                    boolean ativo = idx != -1 && "ATIVO".equalsIgnoreCase(tabelaFuncStatus.get(idx));
                    moduloAutoridades.aplicarAutoridadesCarregadas(idFunc, ativo, entry.getValue());
                }
            }

            tabelaFornRazaoSocial.clear(); tabelaFornRazaoSocial.addAll(tempFornRazao);
            tabelaFornCnpj.clear(); tabelaFornCnpj.addAll(tempFornCnpj);
            tabelaFornUf.clear(); tabelaFornUf.addAll(tempFornUf);
            tabelaFornProdutoPrincipal.clear(); tabelaFornProdutoPrincipal.addAll(tempFornProd);
            tabelaFornValorFatura.clear(); tabelaFornValorFatura.addAll(tempFornFatura);
            tabelaFornMargemLucroPct.clear(); tabelaFornMargemLucroPct.addAll(tempFornMargem);
            tabelaFornFaturaPaga.clear(); tabelaFornFaturaPaga.addAll(tempFornPago);

            tabelaProdNome.clear(); tabelaProdNome.addAll(tempProdNome);
            tabelaProdNcm.clear(); tabelaProdNcm.addAll(tempProdNcm);
            tabelaProdPrecoVenda.clear(); tabelaProdPrecoVenda.addAll(tempProdPreco);
            tabelaProdQtdEstoque.clear(); tabelaProdQtdEstoque.addAll(tempProdEstoque);

            tabelaClienteCpf.clear(); tabelaClienteCpf.addAll(tempClienteCpf);
            tabelaClienteNome.clear(); tabelaClienteNome.addAll(tempClienteNome);

            tabelaTransacaoTipo.clear(); tabelaTransacaoTipo.addAll(tempTransTipo);
            tabelaTransacaoEnvolvidos.clear(); tabelaTransacaoEnvolvidos.addAll(tempTransEnvolvidos);
            tabelaTransacaoDescricao.clear(); tabelaTransacaoDescricao.addAll(tempTransDesc);
            tabelaTransacaoValor.clear(); tabelaTransacaoValor.addAll(tempTransValor);

            caixa.definirSaldo(tempCaixa);
        }
    }

    // Alíquotas de referência para operações internas em SP e interestaduais
    public static final double ALIQUOTA_ICMS_INTERNO_SP_2026 = 0.18;
    public static final double ALIQUOTA_ICMS_INTERNO_SP_2027 = 0.19;
    public static final double ALIQUOTA_ICMS_INTERESTADUAL_PADRAO = 0.12;

    // Substitui ';' e quebras de linha para não corromper arquivos delimitados por ';'
    public static String sanitizarCampoTxt(String valor) {
        if (valor == null) return "";
        return valor.replace(";", ",").replace("\r", " ").replace("\n", " ").trim();
    }

    // Calcula a alíquota de ICMS conforme UFs de origem/destino e exercício fiscal
    public static double calcularAliquotaIcmsPorOperacao(String codigoNcmProduto, String ufOrigemOperacao, String ufDestinoOperacao, int anoExercicioFiscal) {
        double aliquotaInternaVigente = (anoExercicioFiscal >= 2027) ? ALIQUOTA_ICMS_INTERNO_SP_2027 : ALIQUOTA_ICMS_INTERNO_SP_2026;
        if (ufOrigemOperacao != null && ufDestinoOperacao != null
                && ufOrigemOperacao.trim().equalsIgnoreCase("SP")
                && ufDestinoOperacao.trim().equalsIgnoreCase("SP")) {
            return aliquotaInternaVigente;
        }
        return ALIQUOTA_ICMS_INTERESTADUAL_PADRAO;
    }

    // Retorna o salário base padrão conforme o cargo do funcionário
    public static double obterSalarioBasePorCargo(String cargoFuncionario) {
        if (cargoFuncionario == null) return 2200.00;
        if (cargoFuncionario.equalsIgnoreCase("GERENTE")) return 5500.00;
        if (cargoFuncionario.equalsIgnoreCase("FINANCEIRO")) return 4800.00;
        return 2200.00;
    }

    // Gera senha temporária padrão para novos cadastros, readmissões ou resets
    public static String gerarSenhaTemporaria() {
        return "Temp@2026";
    }

    // Formata valores decimais no padrão monetário brasileiro ("R$ 0,00" / "R$ 0.00")
    public static String formatarValorMonetarioReais(double valorNumerico) {
        double valorSeguro = Double.isFinite(valorNumerico) ? valorNumerico : 0.0;
        return String.format("R$ %.2f", valorSeguro);
    }

    // Lê uma linha do Scanner com segurança; se o fluxo terminar, retorna "SAIR"
    private static String lerLinhaSegura(Scanner leitorEntrada) {
        if (leitorEntrada == null || !leitorEntrada.hasNextLine()) return "SAIR";
        return leitorEntrada.nextLine().trim();
    }

    // Identifica se o usuário digitou comando de cancelamento/retorno (0, -0 ou SAIR)
    private static boolean isComandoCancelamento(String entrada) {
        if (entrada == null) return true;
        String limpa = entrada.trim();
        return limpa.equals("0") || limpa.equals("-0") || limpa.equalsIgnoreCase("SAIR");
    }

    // Verifica se todos os caracteres de uma sequência numérica são iguais (ex.: 11111111111)
    private static boolean todosDigitosIguais(String sequencia) {
        if (sequencia == null || sequencia.isEmpty()) return true;
        char primeiro = sequencia.charAt(0);
        for (int i = 1; i < sequencia.length(); i++) {
            if (sequencia.charAt(i) != primeiro) return false;
        }
        return true;
    }

    // Valida CPF de 11 dígitos pelos dois dígitos verificadores
    private static boolean isCpfValido(String cpfSomenteNumeros) {
        if (cpfSomenteNumeros == null || !cpfSomenteNumeros.matches("^[0-9]{11}$") || todosDigitosIguais(cpfSomenteNumeros)) {
            return false;
        }
        int soma1 = 0, soma2 = 0;
        for (int i = 0; i < 9; i++) {
            int digito = cpfSomenteNumeros.charAt(i) - '0';
            soma1 += digito * (10 - i);
            soma2 += digito * (11 - i);
        }
        int dv1 = 11 - (soma1 % 11);
        if (dv1 >= 10) dv1 = 0;
        soma2 += dv1 * 2;
        int dv2 = 11 - (soma2 % 11);
        if (dv2 >= 10) dv2 = 0;
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

    // Lê e valida sigla de unidade federativa brasileira (UF)
    public static String validarELerSiglaEstadoUf(Scanner leitorEntrada, String mensagemPrompt) {
        List<String> ufs = Arrays.asList(
            "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG",
            "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO"
        );
        while (true) {
            System.out.print(mensagemPrompt);
            String sigla = lerLinhaSegura(leitorEntrada).toUpperCase();
            if (isComandoCancelamento(sigla)) return "CANCELAR";
            if (ufs.contains(sigla)) return sigla;
            System.out.println(CoresAnsi.erro("[Validação]: UF inválida.\n"));
        }
    }

    // Lê número inteiro não negativo; retorna -999 se o usuário digitar 0 ou SAIR
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

    // Lê e valida valor decimal monetário (aceitando vírgula ou ponto e até 4 casas decimais)
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
            double aliquotaIcms = calcularAliquotaIcmsPorOperacao(tabelaProdNcm.get(0), "SP", "SP", anoExercicioFiscal);
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
                moduloAutoridades,
                tabelaFuncId, tabelaFuncNome, tabelaFuncCpf, tabelaFuncCargo, tabelaFuncSenha,
                tabelaFuncSalario, tabelaFuncPago, tabelaFuncStatus, tabelaFuncMotivoInativo,
                tabelaFornRazaoSocial, tabelaFornCnpj, tabelaFornUf, tabelaFornProdutoPrincipal,
                tabelaFornValorFatura, tabelaFornMargemLucroPct, tabelaFornFaturaPaga,
                tabelaProdNome, tabelaProdNcm, tabelaProdPrecoVenda, tabelaProdQtdEstoque,
                tabelaClienteCpf, tabelaClienteNome,
                tabelaTransacaoTipo, tabelaTransacaoEnvolvidos, tabelaTransacaoDescricao, tabelaTransacaoValor,
                caixa
        );

        auditoria.registrar("SISTEMA", "Base inicializada via '" + ARQUIVO_PRE_CADASTROS + "' e salva em '" + ARQUIVO_BACKUP_OFICIAL + "'");
        System.out.println(CoresAnsi.sucesso(" Base inicializada (" + tabelaProdNome.size() + " itens, " + tabelaTransacaoTipo.size() + " lançamentos)."));
        System.out.println(CoresAnsi.colorir(CoresAnsi.CIANO, " Pré-cadastros externos em: '" + ARQUIVO_PRE_CADASTROS + "'"));
        System.out.println(CoresAnsi.colorir(CoresAnsi.CIANO, " Arquivo editável disponível: '" + ARQUIVO_BACKUP_OFICIAL + "'"));
        System.out.println(CoresAnsi.colorir(CoresAnsi.CIANO, " Log de entradas/saídas em: '" + ARQUIVO_LOG_DADOS + "'\n"));
    }

    // Exibe os dados do usuário logado e permite alterar a própria senha com persistência automática
    public static void exibirAbaMeuPerfil(
            Scanner leitorEntrada,
            ModuloSessao sessao,
            ModuloAuditoria auditoria,
            ModuloCaixa caixa,
            ModuloPersistenciaTxt persistencia,
            ModuloAutoridades moduloAutoridades,
            List<String> tabelaFuncId, List<String> tabelaFuncNome, List<String> tabelaFuncCpf,
            List<String> tabelaFuncCargo, List<String> tabelaFuncSenha,
            List<Double> tabelaFuncSalario, List<Boolean> tabelaFuncPago,
            List<String> tabelaFuncStatus, List<String> tabelaFuncMotivoInativo,
            List<String> tabelaFornRazaoSocial, List<String> tabelaFornCnpj, List<String> tabelaFornUf,
            List<String> tabelaFornProdutoPrincipal, List<Double> tabelaFornValorFatura,
            List<Double> tabelaFornMargemLucroPct, List<Boolean> tabelaFornFaturaPaga,
            List<String> tabelaProdNome, List<String> tabelaProdNcm, List<Double> tabelaProdPrecoVenda,
            List<Integer> tabelaProdQtdEstoque, List<String> tabelaClienteCpf, List<String> tabelaClienteNome,
            List<String> tabelaTransacaoTipo, List<String> tabelaTransacaoEnvolvidos,
            List<String> tabelaTransacaoDescricao, List<Double> tabelaTransacaoValor) {

        boolean abaPerfilAtiva = true;
        while (abaPerfilAtiva) {
            if (!sessao.validarOuRevogarSessao()) return;

            // Localiza o registro ativo correspondente ao usuário da sessão
            int indice = -1;
            for (int i = 0; i < tabelaFuncId.size(); i++) {
                if (tabelaFuncNome.get(i).equals(sessao.obterNomeVigente()) && tabelaFuncStatus.get(i).equalsIgnoreCase("ATIVO")) {
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
            System.out.printf("│ Perfil / Cargo   : %s\n", tabelaFuncCargo.get(indice));
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

                // Confere se a senha atual informada bate com a cadastrada
                if (!senhaAtualInformada.equals(tabelaFuncSenha.get(indice))) {
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

                // Atualiza a senha na tabela em memória e registra nos logs
                tabelaFuncSenha.set(indice, novaSenha);
                caixa.registrarMovimentoDados("ENTRADA_DADOS", "Usuário: " + tabelaFuncNome.get(indice), "Atualização de senha em Meu Perfil");
                auditoria.registrar(tabelaFuncNome.get(indice), "Alterou a própria senha em Meu Perfil");

                // Persiste a alteração automaticamente no arquivo de backup editável
                boolean salvouTxt = persistencia.salvarBackupEditavel(
                        moduloAutoridades,
                        tabelaFuncId, tabelaFuncNome, tabelaFuncCpf, tabelaFuncCargo, tabelaFuncSenha,
                        tabelaFuncSalario, tabelaFuncPago, tabelaFuncStatus, tabelaFuncMotivoInativo,
                        tabelaFornRazaoSocial, tabelaFornCnpj, tabelaFornUf, tabelaFornProdutoPrincipal,
                        tabelaFornValorFatura, tabelaFornMargemLucroPct, tabelaFornFaturaPaga,
                        tabelaProdNome, tabelaProdNcm, tabelaProdPrecoVenda, tabelaProdQtdEstoque,
                        tabelaClienteCpf, tabelaClienteNome,
                        tabelaTransacaoTipo, tabelaTransacaoEnvolvidos, tabelaTransacaoDescricao, tabelaTransacaoValor,
                        caixa
                );

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

    // Menu de RH: listagem paginada com filtro e cadastro de novos funcionários
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
            List<String> tabelaTransacaoTipo, List<String> tabelaTransacaoEnvolvidos,
            List<String> tabelaTransacaoDescricao, List<Double> tabelaTransacaoValor,
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

        boolean menuRhAtivo = true;
        String filtroFuncAtivo = "";

        while (menuRhAtivo) {
            if (!sessao.validarOuRevogarSessao()) return;

            System.out.println(CoresAnsi.titulo("\n================================================================"));
            System.out.println(CoresAnsi.titulo("                      GESTÃO DE CADASTROS (RH)                  "));
            System.out.println(CoresAnsi.titulo("================================================================"));
            System.out.println("[1] Listar registros (Paginado 25/pág)");
            if (sessao.temAutoridade(Autoridade.RH_GERIR_CADASTROS)) {
                System.out.println("[2] Novo registro");
            }
            System.out.println("[3] Voltar");
            System.out.println(CoresAnsi.titulo("================================================================"));

            int opcaoEscolhida = validarELerNumeroInteiro(leitorEntrada, "Selecione: ");
            if (opcaoEscolhida == 3 || opcaoEscolhida == -999) {
                menuRhAtivo = false;
            } else if (opcaoEscolhida == 1) {
                if (tabelaFuncNome.isEmpty()) {
                    System.out.println(CoresAnsi.aviso(" Nenhum registro disponível.\n"));
                    continue;
                }
                int paginaAtual = 1;
                final int ITENS_POR_PAGINA = 25;
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
                    System.out.println(CoresAnsi.titulo("\n--- REGISTROS [" + etiquetaFiltro + "] (Página " + paginaAtual + " de " + totalPaginas + " | Encontrados: " + totalFiltrados + ") ---"));

                    if (totalFiltrados == 0) {
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
                    System.out.println("[N] Próxima | [P] Anterior | [F] Filtrar | [L] Limpar Filtro | [NÚMERO] Detalhes | [0] Voltar");
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
            } else if (opcaoEscolhida == 2) {
                if (!sessao.exigirAutoridade(Autoridade.RH_GERIR_CADASTROS, "Sem autoridade para cadastrar funcionários.")) continue;

                // Gera ponto de rollback antes de adicionar novo registro
                persistencia.executarRollbackAutomatico(
                        moduloAutoridades,
                        tabelaFuncId, tabelaFuncNome, tabelaFuncCpf, tabelaFuncCargo, tabelaFuncSenha,
                        tabelaFuncSalario, tabelaFuncPago, tabelaFuncStatus, tabelaFuncMotivoInativo,
                        tabelaFornRazaoSocial, tabelaFornCnpj, tabelaFornUf, tabelaFornProdutoPrincipal,
                        tabelaFornValorFatura, tabelaFornMargemLucroPct, tabelaFornFaturaPaga,
                        tabelaProdNome, tabelaProdNcm, tabelaProdPrecoVenda, tabelaProdQtdEstoque,
                        tabelaClienteCpf, tabelaClienteNome,
                        tabelaTransacaoTipo, tabelaTransacaoEnvolvidos, tabelaTransacaoDescricao, tabelaTransacaoValor,
                        caixa
                );

                String novoIdFuncional;
                while (true) {
                    System.out.print("Novo ID (3 dígitos, ex: 009) [0 para cancelar]: ");
                    novoIdFuncional = lerLinhaSegura(leitorEntrada).toUpperCase();
                    if (isComandoCancelamento(novoIdFuncional)) break;
                    if (novoIdFuncional.matches("^[0-9]{3}$") && !novoIdFuncional.equals("000") && !tabelaFuncId.contains(novoIdFuncional)) break;
                    System.out.println(CoresAnsi.erro("ID inválido ou já cadastrado.\n"));
                }
                if (isComandoCancelamento(novoIdFuncional)) continue;

                String nomeCompleto = validarELerTextoAlfabetico(leitorEntrada, "Nome completo: ");
                if (nomeCompleto.equals("CANCELAR")) continue;

                String cpfValidado = validarEFormatarCpf(leitorEntrada);
                if (cpfValidado.equals("CANCELAR")) continue;
                if (tabelaFuncCpf.contains(cpfValidado)) {
                    System.out.println(CoresAnsi.erro("[Duplicidade]: Já existe um registro com este CPF.\n"));
                    continue;
                }

                int opcaoCargo = validarELerNumeroInteiro(leitorEntrada, "Nível de acesso [1 - Gerente | 2 - Operador | 3 - Financeiro] [0 para cancelar]: ");
                if (opcaoCargo == -999) continue;
                if (opcaoCargo < 1 || opcaoCargo > 3) {
                    System.out.println(CoresAnsi.erro("Opção inválida.\n"));
                    continue;
                }
                String cargoDefinido = (opcaoCargo == 1) ? "GERENTE" : (opcaoCargo == 3 ? "FINANCEIRO" : "OPERADOR");
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
            } else {
                System.out.println(CoresAnsi.erro("Opção indisponível.\n"));
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
                System.out.printf("[%2d] %s %-26s -> %s\n",
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

    // Exibe detalhes de um funcionário e ações de pagamento, edição, inativação, readmissão, senha e autoridades
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
                    System.out.println(CoresAnsi.aviso("ℹ️ Registro já está ativo.\n"));
                    continue;
                }
                int opcaoCargo = validarELerNumeroInteiro(leitorEntrada, "Nível de acesso [1 - Gerente | 2 - Operador | 3 - Financeiro] [0 para cancelar]: ");
                if (opcaoCargo == -999) continue;
                if (opcaoCargo < 1 || opcaoCargo > 3) {
                    System.out.println(CoresAnsi.erro("Opção inválida.\n"));
                    continue;
                }
                String novoCargo = (opcaoCargo == 1) ? "GERENTE" : (opcaoCargo == 3 ? "FINANCEIRO" : "OPERADOR");
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
            } else if (acaoEscolhida == 0 || acaoEscolhida == -999) {
                modalAberto = false;
            } else {
                System.out.println(CoresAnsi.erro("Opção indisponível.\n"));
            }
        }
    }

    // Menu de fornecedores: listagem paginada e registro de compras com entrada no estoque
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
            List<String> tabelaTransacaoTipo, List<String> tabelaTransacaoEnvolvidos,
            List<String> tabelaTransacaoDescricao, List<Double> tabelaTransacaoValor,
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

        boolean menuFornecedoresAtivo = true;
        while (menuFornecedoresAtivo) {
            System.out.println(CoresAnsi.titulo("\n================================================================"));
            System.out.println(CoresAnsi.titulo("                   CONTROLE DE FORNECIMENTO                     "));
            System.out.println(CoresAnsi.titulo("================================================================"));
            System.out.println("[1] Listar itens (Paginado 25/pág)");
            if (sessao.temAutoridade(Autoridade.FORN_GERIR_OPERACOES)) {
                System.out.println("[2] Registrar operação");
            }
            System.out.println("[3] Voltar");
            System.out.println(CoresAnsi.titulo("================================================================"));

            int opcaoEscolhida = validarELerNumeroInteiro(leitorEntrada, "Selecione: ");
            if (opcaoEscolhida == 3 || opcaoEscolhida == -999) {
                menuFornecedoresAtivo = false;
            } else if (opcaoEscolhida == 1) {
                if (tabelaFornRazaoSocial.isEmpty()) {
                    System.out.println(CoresAnsi.aviso(" Nenhum registro disponível.\n"));
                    continue;
                }
                int paginaAtual = 1;
                final int ITENS_POR_PAGINA = 25;
                boolean navegandoLista = true;

                while (navegandoLista) {
                    if (tabelaFornRazaoSocial.isEmpty()) {
                        System.out.println(CoresAnsi.aviso("ℹ️ Todos os registros foram removidos.\n"));
                        break;
                    }
                    int totalPaginas = Math.max(1, (int) Math.ceil((double) tabelaFornRazaoSocial.size() / ITENS_POR_PAGINA));
                    if (paginaAtual > totalPaginas) paginaAtual = totalPaginas;

                    System.out.println(CoresAnsi.titulo("\n--- REGISTROS DE FORNECIMENTO (Página " + paginaAtual + " de " + totalPaginas + ") ---"));
                    int indiceInicial = (paginaAtual - 1) * ITENS_POR_PAGINA;
                    int indiceFinal = Math.min(indiceInicial + ITENS_POR_PAGINA, tabelaFornRazaoSocial.size());

                    for (int i = indiceInicial; i < indiceFinal; i++) {
                        String etiquetaFatura = tabelaFornFaturaPaga.get(i) ? CoresAnsi.sucesso("QUITADO ") : CoresAnsi.aviso("PENDENTE ");
                        String valorExibido = sessao.exibirValorCondicional(Autoridade.FIN_VER_VALORES_SENSIVEIS, tabelaFornValorFatura.get(i));
                        System.out.printf("[%d] %s | CNPJ: %s | UF: %s | Valor: %s [%s]\n",
                                (i + 1), tabelaFornRazaoSocial.get(i), tabelaFornCnpj.get(i), tabelaFornUf.get(i),
                                valorExibido, etiquetaFatura);
                    }
                    System.out.println("----------------------------------------------------------------");
                    System.out.println("[N] Próxima Página | [P] Página Anterior | [NÚMERO] Detalhes | [0] Voltar");
                    System.out.print("Comando: ");
                    String comandoUsuario = lerLinhaSegura(leitorEntrada).toUpperCase();

                    if (comandoUsuario.equals("N") && paginaAtual < totalPaginas) paginaAtual++;
                    else if (comandoUsuario.equals("P") && paginaAtual > 1) paginaAtual--;
                    else if (isComandoCancelamento(comandoUsuario)) navegandoLista = false;
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
            } else if (opcaoEscolhida == 2) {
                if (!sessao.exigirAutoridade(Autoridade.FORN_GERIR_OPERACOES, "Sem autoridade para registrar operação de fornecimento.")) continue;

                String razaoSocial = validarELerTextoAlfabetico(leitorEntrada, "Razão Social: ");
                if (razaoSocial.equals("CANCELAR")) continue;

                String cnpjValidado = validarEFormatarCnpj(leitorEntrada);
                if (cnpjValidado.equals("CANCELAR")) continue;
                if (tabelaFornCnpj.contains(cnpjValidado)) {
                    System.out.println(CoresAnsi.erro("[Duplicidade]: Já existe um registro com este CNPJ.\n"));
                    continue;
                }

                String siglaUf = validarELerSiglaEstadoUf(leitorEntrada, "Estado (UF): ");
                if (siglaUf.equals("CANCELAR")) continue;

                String nomeProduto = validarELerTextoLivreObrigatorio(leitorEntrada, "Descrição do item [0 para cancelar]: ");
                if (nomeProduto.equals("CANCELAR")) continue;

                double custoUnitario = validarELerValorDecimalMonetario(leitorEntrada, "Custo unitário (R$) [0 para cancelar]: ", false);
                if (custoUnitario <= 0) continue;

                int quantidadeCompra = validarELerQuantidadePositiva(leitorEntrada, "Quantidade [0 para cancelar]: ");
                if (quantidadeCompra <= 0) continue;

                double valorTotalFatura = custoUnitario * quantidadeCompra;
                if (!Double.isFinite(valorTotalFatura) || valorTotalFatura <= 0.0) {
                    System.out.println(CoresAnsi.erro("Valor total inválido.\n"));
                    continue;
                }

                // Debita do caixa antes de consolidar a compra e a entrada de estoque
                boolean debitou = caixa.registrarSaida(
                        valorTotalFatura,
                        "COMPRA / ABASTECIMENTO",
                        "Fornecedor: " + razaoSocial + " (" + siglaUf + ")",
                        "Entrada em estoque: " + quantidadeCompra + "x " + nomeProduto
                );
                if (!debitou) continue;

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
            } else {
                System.out.println(CoresAnsi.erro("Opção indisponível.\n"));
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
                String nomeCliente = "CLIENTE AVULSO";
                if (envolvidos.contains("Cliente:")) {
                    int inicio = envolvidos.indexOf("Cliente:") + 8;
                    int fim = envolvidos.indexOf(" (CPF:");
                    if (fim == -1) fim = envolvidos.indexOf(" |");
                    if (fim == -1) fim = envolvidos.length();
                    nomeCliente = envolvidos.substring(inicio, fim).trim().toUpperCase();
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

    // Submenu de operações de estoque, cadastro de itens, lançamentos, relatórios, logs e backup
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

            System.out.println(CoresAnsi.titulo("\n================================================================"));
            System.out.println(CoresAnsi.titulo("                     OPERAÇÕES E REGISTROS                      "));
            System.out.println(CoresAnsi.titulo("================================================================"));
            if (sessao.temAutoridade(Autoridade.EST_VER_CATALOGO)) {
                System.out.println("[1] Catálogo e saldos (Paginado 25/pág)");
            }
            if (sessao.temAutoridade(Autoridade.EST_GERIR_CATALOGO)) {
                System.out.println("[2] Cadastrar item");
            }
            if (sessao.temAutoridade(Autoridade.FIN_VER_VALORES_SENSIVEIS)) {
                System.out.println("[3] Histórico de lançamentos (Paginado 25/pág)");
                System.out.println("[4] Relatório por cliente (.TXT)");
                System.out.println("[5] Consolidado financeiro");
            }
            if (sessao.temAutoridade(Autoridade.SIS_AUDITORIA_E_BACKUP)) {
                System.out.println("[6] Registros de auditoria (Paginado 25/pág)");
                System.out.println("[7] Salvar arquivo .TXT");
                System.out.println("[8] Carregar arquivo .TXT");
            }
            System.out.println("[9] Voltar");
            if (sessao.temAutoridade(Autoridade.LOG_VER_E_EXPORTAR)) {
                System.out.println("[10] Consultar/filtrar log de entradas e saídas (.TXT)");
            }
            if (sessao.temAutoridade(Autoridade.EST_VER_CATALOGO)) {
                System.out.println("[11] Histórico de movimentações de estoque");
            }
            System.out.println(CoresAnsi.titulo("================================================================"));

            int opcaoEscolhida = validarELerNumeroInteiro(leitorEntrada, "Selecione: ");
            if (opcaoEscolhida == 9 || opcaoEscolhida == -999) {
                menuFinanceiroAtivo = false;
            } else if (opcaoEscolhida == 10) {
                if (!sessao.exigirAutoridade(Autoridade.LOG_VER_E_EXPORTAR, "Sem autoridade para consultar/exportar log.")) continue;
                exibirLogEntradasSaidasPaginado(leitorEntrada, sessao, auditoria, caixa, logDados);
            } else if (opcaoEscolhida == 11) {
                if (!sessao.exigirAutoridade(Autoridade.EST_VER_CATALOGO, "Sem autoridade para consultar movimentações de estoque.")) continue;
                exibirMovimentacoesEstoquePaginado(leitorEntrada, movEstoque);
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
                gerenciarCatalogoProdutosPaginado(leitorEntrada, sessao, auditoria, caixa, movEstoque, tabelaProdNome, tabelaProdNcm, tabelaProdPrecoVenda, tabelaProdQtdEstoque);
            } else if (opcaoEscolhida == 2) {
                if (!sessao.exigirAutoridade(Autoridade.EST_GERIR_CATALOGO, "Sem autoridade para cadastrar item.")) continue;
                persistencia.executarRollbackAutomatico(
                        moduloAutoridades,
                        tabelaFuncId, tabelaFuncNome, tabelaFuncCpf, tabelaFuncCargo, tabelaFuncSenha,
                        tabelaFuncSalario, tabelaFuncPago, tabelaFuncStatus, tabelaFuncMotivoInativo,
                        tabelaFornRazaoSocial, tabelaFornCnpj, tabelaFornUf, tabelaFornProdutoPrincipal,
                        tabelaFornValorFatura, tabelaFornMargemLucroPct, tabelaFornFaturaPaga,
                        tabelaProdNome, tabelaProdNcm, tabelaProdPrecoVenda, tabelaProdQtdEstoque,
                        tabelaClienteCpf, tabelaClienteNome,
                        tabelaTransacaoTipo, tabelaTransacaoEnvolvidos, tabelaTransacaoDescricao, tabelaTransacaoValor,
                        caixa
                );

                String nomeProduto = validarELerTextoLivreObrigatorio(leitorEntrada, "Nome do item [0 para cancelar]: ");
                if (nomeProduto.equals("CANCELAR")) continue;
                if (tabelaProdNome.contains(nomeProduto)) {
                    System.out.println(CoresAnsi.erro("[Duplicidade]: Já existe um item com o nome '" + nomeProduto + "'.\n"));
                    continue;
                }

                double precoVenda = validarELerValorDecimalMonetario(leitorEntrada, "Preço unitário (R$) [0 para cancelar]: ", false);
                if (precoVenda <= 0) continue;

                int estoqueInicial = validarELerInteiroNaoNegativoComSair(leitorEntrada, "Quantidade inicial [SAIR para cancelar]: ");
                if (estoqueInicial == -999) continue;

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
            } else if (opcaoEscolhida == 3) {
                if (!sessao.exigirAutoridade(Autoridade.FIN_VER_VALORES_SENSIVEIS, "Sem autoridade para ver lançamentos financeiros.")) continue;
                gerenciarHistoricoTransacoesPaginado(leitorEntrada, sessao, auditoria, caixa, movEstoque, tabelaProdNome, tabelaProdQtdEstoque, tabelaTransacaoTipo, tabelaTransacaoEnvolvidos, tabelaTransacaoDescricao, tabelaTransacaoValor);
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
                exibirLogAuditoriaPaginado(leitorEntrada, sessao, auditoria);
            } else if (opcaoEscolhida == 7) {
                persistencia.exportarComConfirmacao(
                        leitorEntrada, sessao, auditoria, moduloAutoridades,
                        tabelaFuncId, tabelaFuncNome, tabelaFuncCpf, tabelaFuncCargo, tabelaFuncSenha,
                        tabelaFuncSalario, tabelaFuncPago, tabelaFuncStatus, tabelaFuncMotivoInativo,
                        tabelaFornRazaoSocial, tabelaFornCnpj, tabelaFornUf, tabelaFornProdutoPrincipal,
                        tabelaFornValorFatura, tabelaFornMargemLucroPct, tabelaFornFaturaPaga,
                        tabelaProdNome, tabelaProdNcm, tabelaProdPrecoVenda, tabelaProdQtdEstoque,
                        tabelaClienteCpf, tabelaClienteNome,
                        tabelaTransacaoTipo, tabelaTransacaoEnvolvidos, tabelaTransacaoDescricao, tabelaTransacaoValor,
                        caixa
                );
            } else if (opcaoEscolhida == 8) {
                persistencia.restaurarBackupOficial(
                        leitorEntrada, sessao, auditoria, moduloAutoridades,
                        tabelaFuncId, tabelaFuncNome, tabelaFuncCpf, tabelaFuncCargo, tabelaFuncSenha,
                        tabelaFuncSalario, tabelaFuncPago, tabelaFuncStatus, tabelaFuncMotivoInativo,
                        tabelaFornRazaoSocial, tabelaFornCnpj, tabelaFornUf, tabelaFornProdutoPrincipal,
                        tabelaFornValorFatura, tabelaFornMargemLucroPct, tabelaFornFaturaPaga,
                        tabelaProdNome, tabelaProdNcm, tabelaProdPrecoVenda, tabelaProdQtdEstoque,
                        tabelaClienteCpf, tabelaClienteNome,
                        tabelaTransacaoTipo, tabelaTransacaoEnvolvidos, tabelaTransacaoDescricao, tabelaTransacaoValor,
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
                    System.out.println(CoresAnsi.aviso("ℹ️ Exportação cancelada.\n"));
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
                        System.out.println(CoresAnsi.aviso("ℹ️ Sobrescrita cancelada. O arquivo existente foi mantido.\n"));
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
                String termoBusca = validarELerTextoLivreObrigatorio(leitorEntrada, "Termo para filtrar [0 para cancelar]: ");
                if (!termoBusca.equals("CANCELAR")) {
                    filtroAtivo = termoBusca;
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

    // Retorna etiqueta visual de alerta para estoque zerado ou baixo
    private static String formatarAlertaEstoque(int saldo) {
        if (saldo <= 0) return CoresAnsi.erro(" [SEM ESTOQUE ]");
        if (saldo <= ESTOQUE_MINIMO_ALERTA) return CoresAnsi.aviso(" [ESTOQUE BAIXO ]");
        return "";
    }

    // Gerencia catálogo paginado com filtro, ficha técnica, edição de preço, ajuste de saldo e remoção
    private static void gerenciarCatalogoProdutosPaginado(
            Scanner leitorEntrada, ModuloSessao sessao, ModuloAuditoria auditoria, ModuloCaixa caixa, ModuloMovimentacaoEstoque movEstoque,
            List<String> tabelaProdNome, List<String> tabelaProdNcm,
            List<Double> tabelaProdPrecoVenda, List<Integer> tabelaProdQtdEstoque) {

        if (tabelaProdNome.isEmpty()) {
            System.out.println(CoresAnsi.aviso(" Catálogo vazio.\n"));
            return;
        }

        boolean podeEditar = sessao.temAutoridade(Autoridade.EST_GERIR_CATALOGO);
        boolean podeRemover = sessao.isGerente() || podeEditar;

        int paginaAtual = 1;
        final int ITENS_POR_PAGINA = 25;
        String filtroProdutoAtivo = "";
        boolean navegandoLista = true;

        while (navegandoLista) {
            if (tabelaProdNome.isEmpty()) {
                System.out.println(CoresAnsi.aviso("ℹ️ Todos os itens foram removidos.\n"));
                break;
            }

            List<Integer> listaIndicesReaisFiltrados = new ArrayList<>();
            for (int i = 0; i < tabelaProdNome.size(); i++) {
                String nome = tabelaProdNome.get(i);
                String ncm = tabelaProdNcm.get(i);
                if (nome != null && (filtroProdutoAtivo.isEmpty() || nome.toUpperCase().contains(filtroProdutoAtivo) || ncm.toUpperCase().contains(filtroProdutoAtivo))) {
                    listaIndicesReaisFiltrados.add(i);
                }
            }

            int totalFiltrados = listaIndicesReaisFiltrados.size();
            int totalPaginas = Math.max(1, (int) Math.ceil((double) totalFiltrados / ITENS_POR_PAGINA));
            if (paginaAtual > totalPaginas) paginaAtual = totalPaginas;

            String etiquetaFiltro = filtroProdutoAtivo.isEmpty() ? "TODOS OS ITENS" : "FILTRO: '" + filtroProdutoAtivo + "'";
            System.out.println(CoresAnsi.titulo("\n--- CATÁLOGO [" + etiquetaFiltro + "] (Página " + paginaAtual + " de " + totalPaginas + " | Encontrados: " + totalFiltrados + ") ---"));

            if (totalFiltrados == 0) {
                System.out.println(CoresAnsi.aviso(" Nenhum item encontrado para o filtro '" + filtroProdutoAtivo + "'. Pressione [L] para limpar."));
            } else {
                int indiceInicial = (paginaAtual - 1) * ITENS_POR_PAGINA;
                int indiceFinal = Math.min(indiceInicial + ITENS_POR_PAGINA, totalFiltrados);
                for (int i = indiceInicial; i < indiceFinal; i++) {
                    int indiceReal = listaIndicesReaisFiltrados.get(i);
                    int saldo = tabelaProdQtdEstoque.get(indiceReal);
                    System.out.printf("[%d] %s (NCM: %s) | Preço: %s | Saldo: %d un%s\n",
                            (i + 1), tabelaProdNome.get(indiceReal), tabelaProdNcm.get(indiceReal),
                            formatarValorMonetarioReais(tabelaProdPrecoVenda.get(indiceReal)), saldo,
                            formatarAlertaEstoque(saldo));
                }
            }

            System.out.println("----------------------------------------------------------------");
            System.out.println("[N] Próxima | [P] Anterior | [F] Filtrar | [L] Limpar Filtro | [NÚMERO] Detalhes | [0] Voltar");
            System.out.print("Comando: ");
            String comandoUsuario = lerLinhaSegura(leitorEntrada).toUpperCase();

            if (comandoUsuario.equals("N") && paginaAtual < totalPaginas) paginaAtual++;
            else if (comandoUsuario.equals("P") && paginaAtual > 1) paginaAtual--;
            else if (comandoUsuario.equals("F")) {
                String termoBusca = validarELerTextoLivreObrigatorio(leitorEntrada, "Termo para filtrar [0 para cancelar]: ");
                if (!termoBusca.equals("CANCELAR")) {
                    filtroProdutoAtivo = termoBusca;
                    paginaAtual = 1;
                }
            } else if (comandoUsuario.equals("L")) {
                filtroProdutoAtivo = "";
                paginaAtual = 1;
                System.out.println(CoresAnsi.sucesso(" Filtro removido.\n"));
            } else if (isComandoCancelamento(comandoUsuario)) {
                navegandoLista = false;
            } else if (comandoUsuario.matches("^[0-9]+$")) {
                int pos = Integer.parseInt(comandoUsuario) - 1;
                if (pos >= 0 && pos < totalFiltrados) {
                    int indiceSelecionado = listaIndicesReaisFiltrados.get(pos);
                    boolean modalAberto = true;
                    while (modalAberto) {
                        if (indiceSelecionado < 0 || indiceSelecionado >= tabelaProdNome.size()) {
                            System.out.println(CoresAnsi.aviso(" Item já removido.\n"));
                            break;
                        }
                        System.out.println(CoresAnsi.titulo("\n┌────────────────────────────────────────────────────────────────┐"));
                        System.out.println(CoresAnsi.titulo("│                      [DETALHES DO ITEM]                        │"));
                        System.out.println(CoresAnsi.titulo("├────────────────────────────────────────────────────────────────┤"));
                        System.out.printf("│ Item: %s\n", tabelaProdNome.get(indiceSelecionado));
                        System.out.printf("│ Preço: %s | Saldo: %d un%s\n",
                                formatarValorMonetarioReais(tabelaProdPrecoVenda.get(indiceSelecionado)),
                                tabelaProdQtdEstoque.get(indiceSelecionado),
                                formatarAlertaEstoque(tabelaProdQtdEstoque.get(indiceSelecionado)));
                        System.out.println(CoresAnsi.titulo("├────────────────────────────────────────────────────────────────┤"));
                        System.out.println("│ [1] Ver ficha técnica                                          │");
                        if (podeEditar) {
                            System.out.println("│ [2] Editar preço                                               │");
                            System.out.println("│ [3] Ajustar saldo                                              │");
                        }
                        if (podeRemover) {
                            System.out.println("│ [4] Remover item                                               │");
                        }
                        System.out.println("│ [0] Fechar                                                     │");
                        System.out.println(CoresAnsi.titulo("└────────────────────────────────────────────────────────────────┘"));

                        int acaoEscolhida = validarELerNumeroInteiro(leitorEntrada, "Ação: ");
                        if (acaoEscolhida == 1) {
                            System.out.println(CoresAnsi.titulo("\n--- FICHA DO ITEM ---"));
                            System.out.println("• Nome      : " + tabelaProdNome.get(indiceSelecionado));
                            System.out.println("• NCM       : " + tabelaProdNcm.get(indiceSelecionado));
                            System.out.println("• Preço     : " + formatarValorMonetarioReais(tabelaProdPrecoVenda.get(indiceSelecionado)));
                            System.out.println("• Saldo     : " + tabelaProdQtdEstoque.get(indiceSelecionado) + " unidades" + formatarAlertaEstoque(tabelaProdQtdEstoque.get(indiceSelecionado)) + "\n");
                        } else if (acaoEscolhida == 2) {
                            if (!sessao.exigirAutoridade(Autoridade.EST_GERIR_CATALOGO, "Sem autoridade para editar preço.")) continue;
                            double novoPreco = validarELerValorDecimalMonetario(leitorEntrada, "Novo preço (R$) [0 para cancelar]: ", false);
                            if (novoPreco > 0) {
                                tabelaProdPrecoVenda.set(indiceSelecionado, novoPreco);
                                caixa.registrarMovimentoDados("ENTRADA_DADOS", "Operador: " + sessao.obterNomeVigente(), "Novo preço de '" + tabelaProdNome.get(indiceSelecionado) + "': " + formatarValorMonetarioReais(novoPreco));
                                auditoria.registrar(sessao.obterNomeVigente(), "Alterou preço de " + tabelaProdNome.get(indiceSelecionado));
                                System.out.println(CoresAnsi.sucesso(" Preço atualizado.\n"));
                            }
                        } else if (acaoEscolhida == 3) {
                            if (!sessao.exigirAutoridade(Autoridade.EST_GERIR_CATALOGO, "Sem autoridade para ajustar saldo.")) continue;
                            int novoEstoque = validarELerInteiroNaoNegativoComSair(leitorEntrada, "Novo saldo [SAIR para cancelar]: ");
                            if (novoEstoque != -999 && novoEstoque >= 0) {
                                int saldoAnterior = tabelaProdQtdEstoque.get(indiceSelecionado);
                                int delta = novoEstoque - saldoAnterior;
                                tabelaProdQtdEstoque.set(indiceSelecionado, novoEstoque);
                                if (movEstoque != null) {
                                    movEstoque.registrarMovimento("AJUSTE_INVENTARIO", tabelaProdNome.get(indiceSelecionado), delta, novoEstoque, "Ajuste manual", sessao.obterNomeVigente());
                                }
                                String direcao = novoEstoque >= saldoAnterior ? "ENTRADA_DADOS" : "SAIDA_DADOS";
                                caixa.registrarMovimentoDados(direcao, "Operador: " + sessao.obterNomeVigente(), "Saldo de '" + tabelaProdNome.get(indiceSelecionado) + "': " + saldoAnterior + " -> " + novoEstoque + " un");
                                auditoria.registrar(sessao.obterNomeVigente(), "Ajustou saldo de " + tabelaProdNome.get(indiceSelecionado));
                                System.out.println(CoresAnsi.sucesso(" Saldo atualizado.\n"));
                            }
                        } else if (acaoEscolhida == 4) {
                            if (!sessao.exigirAutoridade(Autoridade.EST_GERIR_CATALOGO, "Sem autoridade para remover item.")) continue;
                            String nomeRemovido = tabelaProdNome.get(indiceSelecionado);
                            int saldoRemovido = tabelaProdQtdEstoque.get(indiceSelecionado);
                            tabelaProdNome.remove(indiceSelecionado);
                            tabelaProdNcm.remove(indiceSelecionado);
                            tabelaProdPrecoVenda.remove(indiceSelecionado);
                            tabelaProdQtdEstoque.remove(indiceSelecionado);
                            if (movEstoque != null) {
                                movEstoque.registrarMovimento("REMOCAO_ITEM", nomeRemovido, -saldoRemovido, 0, "Catálogo", sessao.obterNomeVigente());
                            }
                            caixa.registrarMovimentoDados("SAIDA_DADOS", "Operador: " + sessao.obterNomeVigente(), "Remoção de item do catálogo: " + nomeRemovido);
                            auditoria.registrar(sessao.obterNomeVigente(), "Removeu item: " + nomeRemovido);
                            System.out.println(CoresAnsi.aviso(" Item removido.\n"));
                            modalAberto = false;
                        } else if (acaoEscolhida == 0 || acaoEscolhida == -999) {
                            modalAberto = false;
                        } else {
                            System.out.println(CoresAnsi.erro("Opção indisponível.\n"));
                        }
                    }
                } else {
                    System.out.println(CoresAnsi.erro("Item inexistente.\n"));
                }
            } else {
                System.out.println(CoresAnsi.erro("Comando inválido.\n"));
            }
        }
    }

    // Gerencia histórico de lançamentos com paginação, filtro, comprovante, edição de descrição e estorno
    private static void gerenciarHistoricoTransacoesPaginado(
            Scanner leitorEntrada, ModuloSessao sessao, ModuloAuditoria auditoria, ModuloCaixa caixa, ModuloMovimentacaoEstoque movEstoque,
            List<String> tabelaProdNome, List<Integer> tabelaProdQtdEstoque,
            List<String> tabelaTransacaoTipo, List<String> tabelaTransacaoEnvolvidos,
            List<String> tabelaTransacaoDescricao, List<Double> tabelaTransacaoValor) {

        if (tabelaTransacaoTipo.isEmpty()) {
            System.out.println(CoresAnsi.aviso(" Nenhum lançamento registrado.\n"));
            return;
        }

        boolean podeEstornar = sessao.temAutoridade(Autoridade.FIN_PAGAR_E_ESTORNAR);

        int paginaAtual = 1;
        final int ITENS_POR_PAGINA = 25;
        String filtroTransacaoAtivo = "";
        boolean navegandoLista = true;

        while (navegandoLista) {
            if (tabelaTransacaoTipo.isEmpty()) {
                System.out.println(CoresAnsi.aviso("ℹ️ Todos os lançamentos foram removidos.\n"));
                break;
            }

            List<Integer> listaIndicesReaisFiltrados = new ArrayList<>();
            for (int i = 0; i < tabelaTransacaoTipo.size(); i++) {
                String tipo = tabelaTransacaoTipo.get(i);
                String envolvidos = tabelaTransacaoEnvolvidos.get(i);
                String descricao = tabelaTransacaoDescricao.get(i);
                if (filtroTransacaoAtivo.isEmpty() || tipo.toUpperCase().contains(filtroTransacaoAtivo) || envolvidos.toUpperCase().contains(filtroTransacaoAtivo) || descricao.toUpperCase().contains(filtroTransacaoAtivo)) {
                    listaIndicesReaisFiltrados.add(i);
                }
            }

            int totalFiltrados = listaIndicesReaisFiltrados.size();
            int totalPaginas = Math.max(1, (int) Math.ceil((double) totalFiltrados / ITENS_POR_PAGINA));
            if (paginaAtual > totalPaginas) paginaAtual = totalPaginas;

            String etiquetaFiltro = filtroTransacaoAtivo.isEmpty() ? "TODOS OS LANÇAMENTOS" : "FILTRO: '" + filtroTransacaoAtivo + "'";
            System.out.println(CoresAnsi.titulo("\n--- HISTÓRICO DE LANÇAMENTOS [" + etiquetaFiltro + "] (Página " + paginaAtual + " de " + totalPaginas + " | Encontrados: " + totalFiltrados + ") ---"));

            if (totalFiltrados == 0) {
                System.out.println(CoresAnsi.aviso(" Nenhum lançamento corresponde ao filtro '" + filtroTransacaoAtivo + "'. Pressione [L] para limpar."));
            } else {
                int indiceInicial = (paginaAtual - 1) * ITENS_POR_PAGINA;
                int indiceFinal = Math.min(indiceInicial + ITENS_POR_PAGINA, totalFiltrados);
                for (int i = indiceInicial; i < indiceFinal; i++) {
                    int indiceReal = listaIndicesReaisFiltrados.get(i);
                    System.out.printf("[%d] %s | Valor: %s | %s\n",
                            (i + 1), tabelaTransacaoTipo.get(indiceReal),
                            formatarValorMonetarioReais(tabelaTransacaoValor.get(indiceReal)),
                            tabelaTransacaoDescricao.get(indiceReal));
                }
            }

            System.out.println("----------------------------------------------------------------");
            System.out.println("[N] Próxima | [P] Anterior | [F] Filtrar | [L] Limpar Filtro | [NÚMERO] Detalhes | [0] Voltar");
            System.out.print("Comando: ");
            String comandoUsuario = lerLinhaSegura(leitorEntrada).toUpperCase();

            if (comandoUsuario.equals("N") && paginaAtual < totalPaginas) paginaAtual++;
            else if (comandoUsuario.equals("P") && paginaAtual > 1) paginaAtual--;
            else if (comandoUsuario.equals("F")) {
                String termoBusca = validarELerTextoLivreObrigatorio(leitorEntrada, "Termo para filtrar [0 para cancelar]: ");
                if (!termoBusca.equals("CANCELAR")) {
                    filtroTransacaoAtivo = termoBusca;
                    paginaAtual = 1;
                }
            } else if (comandoUsuario.equals("L")) {
                filtroTransacaoAtivo = "";
                paginaAtual = 1;
                System.out.println(CoresAnsi.sucesso(" Filtro removido.\n"));
            } else if (isComandoCancelamento(comandoUsuario)) {
                navegandoLista = false;
            } else if (comandoUsuario.matches("^[0-9]+$")) {
                int pos = Integer.parseInt(comandoUsuario) - 1;
                if (pos >= 0 && pos < totalFiltrados) {
                    int indiceSelecionado = listaIndicesReaisFiltrados.get(pos);
                    boolean modalAberto = true;
                    while (modalAberto) {
                        if (indiceSelecionado < 0 || indiceSelecionado >= tabelaTransacaoTipo.size()) {
                            System.out.println(CoresAnsi.aviso(" Lançamento já removido.\n"));
                            break;
                        }
                        System.out.println(CoresAnsi.titulo("\n┌────────────────────────────────────────────────────────────────┐"));
                        System.out.println(CoresAnsi.titulo("│                    [DETALHES DO LANÇAMENTO]                    │"));
                        System.out.println(CoresAnsi.titulo("├────────────────────────────────────────────────────────────────┤"));
                        System.out.printf("│ Operação: %s | Valor: %s\n", tabelaTransacaoTipo.get(indiceSelecionado), formatarValorMonetarioReais(tabelaTransacaoValor.get(indiceSelecionado)));
                        System.out.printf("│ Envolvidos: %s\n", tabelaTransacaoEnvolvidos.get(indiceSelecionado));
                        System.out.println(CoresAnsi.titulo("├────────────────────────────────────────────────────────────────┤"));
                        System.out.println("│ [1] Ver comprovante                                             │");
                        if (podeEstornar) {
                            System.out.println("│ [2] Editar descrição                                           │");
                            System.out.println("│ [3] Estornar e remover                                         │");
                        }
                        System.out.println("│ [0] Fechar                                                     │");
                        System.out.println(CoresAnsi.titulo("└────────────────────────────────────────────────────────────────┘"));

                        int acaoEscolhida = validarELerNumeroInteiro(leitorEntrada, "Ação: ");
                        if (acaoEscolhida == 1) {
                            System.out.println(CoresAnsi.titulo("\n--- COMPROVANTE #" + (indiceSelecionado + 1) + " ---"));
                            System.out.println("• Tipo       : " + tabelaTransacaoTipo.get(indiceSelecionado));
                            System.out.println("• Envolvidos : " + tabelaTransacaoEnvolvidos.get(indiceSelecionado));
                            System.out.println("• Descrição  : " + tabelaTransacaoDescricao.get(indiceSelecionado));
                            System.out.println("• Valor      : " + formatarValorMonetarioReais(tabelaTransacaoValor.get(indiceSelecionado)));
                            System.out.println("----------------------------------------------------------------\n");
                        } else if (acaoEscolhida == 2) {
                            if (!sessao.exigirAutoridade(Autoridade.FIN_PAGAR_E_ESTORNAR, "Sem autoridade para editar lançamento.")) continue;
                            String novaDescricao = validarELerTextoLivreObrigatorio(leitorEntrada, "Nova descrição [0 para cancelar]: ");
                            if (!novaDescricao.equals("CANCELAR")) {
                                tabelaTransacaoDescricao.set(indiceSelecionado, sanitizarCampoTxt(novaDescricao));
                                auditoria.registrar(sessao.obterNomeVigente(), "Editou descrição do lançamento #" + (indiceSelecionado + 1));
                                System.out.println(CoresAnsi.sucesso(" Descrição atualizada.\n"));
                            }
                        } else if (acaoEscolhida == 3) {
                            if (!sessao.exigirAutoridade(Autoridade.FIN_PAGAR_E_ESTORNAR, "Sem autoridade para estornar lançamento.")) continue;
                            String tipoEstorno = tabelaTransacaoTipo.get(indiceSelecionado);
                            String descEstorno = tabelaTransacaoDescricao.get(indiceSelecionado);
                            if (caixa.estornarLancamento(indiceSelecionado)) {
                                if (movEstoque != null && "VENDA".equalsIgnoreCase(tipoEstorno)) {
                                    movEstoque.registrarMovimento("ESTORNO_VENDA", descEstorno, 0, 0, "Estorno financeiro", sessao.obterNomeVigente());
                                }
                                caixa.registrarMovimentoDados("SAIDA_DADOS", "Operador: " + sessao.obterNomeVigente(), "Estorno/remoção do lançamento: " + descEstorno);
                                auditoria.registrar(sessao.obterNomeVigente(), "Estornou lançamento #" + (indiceSelecionado + 1));
                                System.out.println(CoresAnsi.aviso(" Lançamento estornado e removido.\n"));
                                modalAberto = false;
                            }
                        } else if (acaoEscolhida == 0 || acaoEscolhida == -999) {
                            modalAberto = false;
                        } else {
                            System.out.println(CoresAnsi.erro("Opção indisponível.\n"));
                        }
                    }
                } else {
                    System.out.println(CoresAnsi.erro("Lançamento inexistente.\n"));
                }
            } else {
                System.out.println(CoresAnsi.erro("Comando inválido.\n"));
            }
        }
    }

    // Consulta todas as vendas vinculadas a um CPF informado pelo cliente
    private static void consultarHistoricoClientePorCpf(
            Scanner leitorEntrada,
            List<String> tabelaClienteCpf,
            List<String> tabelaClienteNome,
            List<String> tabelaTransacaoTipo,
            List<String> tabelaTransacaoEnvolvidos,
            List<String> tabelaTransacaoDescricao,
            List<Double> tabelaTransacaoValor) {

        String cpfBusca = validarEFormatarCpf(leitorEntrada);
        if (cpfBusca.equals("CANCELAR")) return;

        int idxCliente = tabelaClienteCpf.indexOf(cpfBusca);
        String nomeCliente = (idxCliente != -1) ? tabelaClienteNome.get(idxCliente) : "CLIENTE NÃO CADASTRADO";

        System.out.println(CoresAnsi.titulo("\n================================================================"));
        System.out.println(CoresAnsi.titulo("                  HISTÓRICO DE COMPRAS DO CLIENTE               "));
        System.out.println(CoresAnsi.titulo("================================================================"));
        System.out.println("• Cliente : " + nomeCliente);
        System.out.println("• CPF     : " + cpfBusca);
        System.out.println("----------------------------------------------------------------");

        int qtdCompras = 0;
        double totalGasto = 0.0;

        for (int i = 0; i < tabelaTransacaoTipo.size(); i++) {
            if ("VENDA".equalsIgnoreCase(tabelaTransacaoTipo.get(i))) {
                String env = tabelaTransacaoEnvolvidos.get(i);
                if (env.contains(cpfBusca) || (idxCliente != -1 && env.toUpperCase().contains(nomeCliente))) {
                    qtdCompras++;
                    double val = tabelaTransacaoValor.get(i);
                    totalGasto += val;
                    System.out.printf("[%d] %s | Valor: %s\n", qtdCompras, tabelaTransacaoDescricao.get(i), formatarValorMonetarioReais(val));
                }
            }
        }

        if (qtdCompras == 0) {
            System.out.println(CoresAnsi.aviso("ℹ️ Nenhuma compra registrada para este CPF até o momento.\n"));
        } else {
            System.out.println("----------------------------------------------------------------");
            System.out.printf("Total de compras: %d | Valor acumulado: %s\n\n", qtdCompras, CoresAnsi.sucesso(formatarValorMonetarioReais(totalGasto)));
        }
    }

    // Retorna a quantidade de um produto já reservada no carrinho atual
    private static int obterQuantidadeReservadaNoCarrinho(int indiceProduto, List<Integer> carrinhoIndicesProdutos, List<Integer> carrinhoQuantidades) {
        int pos = carrinhoIndicesProdutos.indexOf(indiceProduto);
        return (pos != -1) ? carrinhoQuantidades.get(pos) : 0;
    }

    // Gerencia itens do carrinho, cadastro rápido de cliente e finalização da venda com baixa em estoque
    private static boolean gerenciarCarrinhoEFinalizarCompra(
            Scanner leitorEntrada, ModuloCaixa caixa, ModuloMovimentacaoEstoque movEstoque,
            List<Integer> carrinhoIndicesProdutos, List<Integer> carrinhoQuantidades,
            List<String> tabelaProdNome, List<String> tabelaProdNcm, List<Double> tabelaProdPrecoVenda, List<Integer> tabelaProdQtdEstoque,
            List<Double> indicadorReceitaBrutaVendas, List<Double> indicadorCustoMercadoriaVendida, List<Double> indicadorImpostosProvisionados,
            List<String> tabelaClienteCpf, List<String> tabelaClienteNome, int anoExercicioFiscal) {

        boolean gerenciandoCarrinho = true;
        while (gerenciandoCarrinho) {
            if (carrinhoIndicesProdutos.isEmpty()) {
                System.out.println(CoresAnsi.aviso("\n🛒 Seu carrinho está vazio.\n"));
                return false;
            }

            double subtotalGeral = 0.0;
            double totalImpostosGeral = 0.0;
            int totalUnidades = 0;

            System.out.println(CoresAnsi.titulo("\n┌────────────────────────────────────────────────────────────────────────┐"));
            System.out.println(CoresAnsi.titulo("│                       🛒 CARRINHO DE COMPRAS                           │"));
            System.out.println(CoresAnsi.titulo("├────────────────────────────────────────────────────────────────────────┤"));
            for (int i = 0; i < carrinhoIndicesProdutos.size(); i++) {
                int idxProd = carrinhoIndicesProdutos.get(i);
                int qtd = carrinhoQuantidades.get(i);
                double precoUnit = tabelaProdPrecoVenda.get(idxProd);
                double subtotalItem = precoUnit * qtd;
                double aliquota = calcularAliquotaIcmsPorOperacao(tabelaProdNcm.get(idxProd), "SP", "SP", anoExercicioFiscal);

                subtotalGeral += subtotalItem;
                totalImpostosGeral += (subtotalItem * aliquota);
                totalUnidades += qtd;

                System.out.printf("│ [%d] %s | %dx %s = %s\n",
                        (i + 1), tabelaProdNome.get(idxProd), qtd,
                        formatarValorMonetarioReais(precoUnit), formatarValorMonetarioReais(subtotalItem));
            }
            System.out.println(CoresAnsi.titulo("├────────────────────────────────────────────────────────────────────────┤"));
            System.out.printf("│ Total de Itens: %d un | ICMS Embutido: %s | TOTAL A PAGAR: %s\n",
                    totalUnidades, formatarValorMonetarioReais(totalImpostosGeral), CoresAnsi.sucesso(formatarValorMonetarioReais(subtotalGeral)));
            System.out.println(CoresAnsi.titulo("└────────────────────────────────────────────────────────────────────────┘"));
            System.out.println("[K] Finalizar e Pagar Pedido | [R] Remover Item | [E] Esvaziar Carrinho | [0] Continuar Comprando");
            System.out.print("Opção no Carrinho: ");
            String comandoCarrinho = lerLinhaSegura(leitorEntrada).toUpperCase();

            if (isComandoCancelamento(comandoCarrinho)) {
                gerenciandoCarrinho = false;
            } else if (comandoCarrinho.equals("E")) {
                carrinhoIndicesProdutos.clear();
                carrinhoQuantidades.clear();
                System.out.println(CoresAnsi.aviso(" Carrinho esvaziado com sucesso.\n"));
                return false;
            } else if (comandoCarrinho.equals("R")) {
                int itemRemover = validarELerNumeroInteiro(leitorEntrada, "Número do item para remover [0 para cancelar]: ");
                if (itemRemover != -999) {
                    int idxCarrinho = itemRemover - 1;
                    if (idxCarrinho >= 0 && idxCarrinho < carrinhoIndicesProdutos.size()) {
                        String nomeRemovido = tabelaProdNome.get(carrinhoIndicesProdutos.get(idxCarrinho));
                        carrinhoIndicesProdutos.remove(idxCarrinho);
                        carrinhoQuantidades.remove(idxCarrinho);
                        System.out.println(CoresAnsi.aviso(" Item '" + nomeRemovido + "' removido.\n"));
                    } else {
                        System.out.println(CoresAnsi.erro("Item inválido.\n"));
                    }
                }
            } else if (comandoCarrinho.equals("K")) {
                String cpfComprador = validarEFormatarCpf(leitorEntrada);
                if (cpfComprador.equals("CANCELAR")) continue;

                String nomeComprador = "CLIENTE AVULSO";
                int indiceClienteCadastrado = tabelaClienteCpf.indexOf(cpfComprador);
                if (indiceClienteCadastrado != -1) {
                    nomeComprador = tabelaClienteNome.get(indiceClienteCadastrado);
                } else {
                    System.out.println(CoresAnsi.aviso(" CPF não encontrado na base de clientes cadastrados."));
                    System.out.print("Deseja cadastrar este novo cliente agora? [S/N]: ");
                    if (lerLinhaSegura(leitorEntrada).toUpperCase().equals("S")) {
                        String novoNomeCliente = validarELerTextoAlfabetico(leitorEntrada, "Nome completo do novo cliente: ");
                        if (!novoNomeCliente.equals("CANCELAR")) {
                            tabelaClienteCpf.add(cpfComprador);
                            tabelaClienteNome.add(novoNomeCliente);
                            nomeComprador = novoNomeCliente;
                            caixa.registrarMovimentoDados("ENTRADA_DADOS", "Portal Cliente", "Novo cliente cadastrado: " + novoNomeCliente + " (" + cpfComprador + ")");
                            System.out.println(CoresAnsi.sucesso(" Novo cliente cadastrado com sucesso.\n"));
                        } else {
                            System.out.println(CoresAnsi.aviso("ℹ️ Cadastro cancelado. Prosseguindo como CLIENTE AVULSO.\n"));
                        }
                    } else {
                        System.out.println(CoresAnsi.aviso("ℹ️ Prosseguindo como CLIENTE AVULSO.\n"));
                    }
                }

                // Revalida o estoque disponível de todos os itens antes de efetivar o pedido
                for (int i = 0; i < carrinhoIndicesProdutos.size(); i++) {
                    int idxProd = carrinhoIndicesProdutos.get(i);
                    int qtd = carrinhoQuantidades.get(i);
                    if (idxProd < 0 || idxProd >= tabelaProdNome.size() || tabelaProdNome.get(idxProd) == null) {
                        System.out.println(CoresAnsi.erro("\nUm item do carrinho não está mais disponível.\n"));
                        return false;
                    }
                    int estoqueAtualReal = tabelaProdQtdEstoque.get(idxProd);
                    if (qtd <= 0 || estoqueAtualReal < qtd) {
                        System.out.println(CoresAnsi.erro("\nSaldo insuficiente para concluir o pedido."));
                        System.out.printf("• Item: %s\n• Solicitado: %d un | Disponível: %d un\n\n",
                                tabelaProdNome.get(idxProd), qtd, Math.max(0, estoqueAtualReal));
                        return false;
                    }
                }

                // Efetiva baixa no estoque, registra movimentação e monta resumo do pedido
                StringBuilder resumoItensPedido = new StringBuilder();
                for (int i = 0; i < carrinhoIndicesProdutos.size(); i++) {
                    int idxProd = carrinhoIndicesProdutos.get(i);
                    int qtd = carrinhoQuantidades.get(i);
                    int novoSaldo = tabelaProdQtdEstoque.get(idxProd) - qtd;
                    tabelaProdQtdEstoque.set(idxProd, novoSaldo);

                    if (movEstoque != null) {
                        movEstoque.registrarMovimento("SAIDA_VENDA", tabelaProdNome.get(idxProd), -qtd, novoSaldo, "Cliente: " + nomeComprador + " (" + cpfComprador + ")", "PORTAL_CLIENTE");
                    }

                    if (i > 0) resumoItensPedido.append(", ");
                    resumoItensPedido.append(qtd).append("x ").append(tabelaProdNome.get(idxProd));
                }

                indicadorReceitaBrutaVendas.set(0, indicadorReceitaBrutaVendas.get(0) + subtotalGeral);
                indicadorCustoMercadoriaVendida.set(0, indicadorCustoMercadoriaVendida.get(0) + (subtotalGeral * 0.45));
                indicadorImpostosProvisionados.set(0, indicadorImpostosProvisionados.get(0) + totalImpostosGeral);

                caixa.registrarEntrada(
                        subtotalGeral,
                        "VENDA",
                        "Cliente: " + nomeComprador + " (CPF: " + cpfComprador + ")",
                        "Pedido Carrinho (" + carrinhoIndicesProdutos.size() + " itens): " + resumoItensPedido.toString()
                );

                carrinhoIndicesProdutos.clear();
                carrinhoQuantidades.clear();

                System.out.println(CoresAnsi.sucesso("\n================================================================"));
                System.out.println(CoresAnsi.sucesso("               PEDIDO CONCLUÍDO COM SUCESSO!                  "));
                System.out.println(CoresAnsi.sucesso("================================================================"));
                System.out.println("• Cliente       : " + nomeComprador + " (CPF: " + cpfComprador + ")");
                System.out.println("• Itens         : " + resumoItensPedido.toString());
                System.out.println("• Total Pago    : " + CoresAnsi.sucesso(formatarValorMonetarioReais(subtotalGeral)));
                System.out.println("• ICMS Embutido : " + formatarValorMonetarioReais(totalImpostosGeral));
                System.out.println(CoresAnsi.sucesso("================================================================\n"));
                return true;
            } else {
                System.out.println(CoresAnsi.erro("Opção inválida no carrinho.\n"));
            }
        }
        return false;
    }

    // Vitrine paginada para o cliente adicionar produtos ao carrinho, abrir carrinho ou consultar histórico
    public static void executarPortalComprasCliente(
            Scanner leitorEntrada, ModuloCaixa caixa, ModuloPersistenciaTxt persistencia,
            ModuloAutoridades moduloAutoridades, ModuloMovimentacaoEstoque movEstoque,
            List<String> tabelaFuncId, List<String> tabelaFuncNome, List<String> tabelaFuncCpf,
            List<String> tabelaFuncCargo, List<String> tabelaFuncSenha, List<Double> tabelaFuncSalario,
            List<Boolean> tabelaFuncPago, List<String> tabelaFuncStatus, List<String> tabelaFuncMotivoInativo,
            List<String> tabelaFornRazaoSocial, List<String> tabelaFornCnpj, List<String> tabelaFornUf,
            List<String> tabelaFornProdutoPrincipal, List<Double> tabelaFornValorFatura,
            List<Double> tabelaFornMargemLucroPct, List<Boolean> tabelaFornFaturaPaga,
            List<String> tabelaProdNome, List<String> tabelaProdNcm, List<Double> tabelaProdPrecoVenda, List<Integer> tabelaProdQtdEstoque,
            List<Double> indicadorReceitaBrutaVendas, List<Double> indicadorCustoMercadoriaVendida, List<Double> indicadorImpostosProvisionados,
            List<String> tabelaClienteCpf, List<String> tabelaClienteNome,
            List<String> tabelaTransacaoTipo, List<String> tabelaTransacaoEnvolvidos,
            List<String> tabelaTransacaoDescricao, List<Double> tabelaTransacaoValor,
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

        if (tabelaProdNome.isEmpty()) {
            System.out.println(CoresAnsi.aviso(" Nenhum item disponível na vitrine.\n"));
            return;
        }

        int paginaAtual = 1;
        final int ITENS_POR_PAGINA = 25;
        String filtroVitrineAtivo = "";
        boolean portalClienteAtivo = true;

        List<Integer> carrinhoIndicesProdutos = new ArrayList<>();
        List<Integer> carrinhoQuantidades = new ArrayList<>();

        while (portalClienteAtivo) {
            List<Integer> listaIndicesReaisFiltrados = new ArrayList<>();
            for (int i = 0; i < tabelaProdNome.size(); i++) {
                String nome = tabelaProdNome.get(i);
                if (nome != null && (filtroVitrineAtivo.isEmpty() || nome.toUpperCase().contains(filtroVitrineAtivo))) {
                    listaIndicesReaisFiltrados.add(i);
                }
            }

            int totalFiltrados = listaIndicesReaisFiltrados.size();
            int totalPaginas = Math.max(1, (int) Math.ceil((double) totalFiltrados / ITENS_POR_PAGINA));
            if (paginaAtual > totalPaginas) paginaAtual = totalPaginas;

            int qtdTotalNoCarrinho = 0;
            double valorTotalNoCarrinho = 0.0;
            for (int i = 0; i < carrinhoIndicesProdutos.size(); i++) {
                int idx = carrinhoIndicesProdutos.get(i);
                int q = carrinhoQuantidades.get(i);
                qtdTotalNoCarrinho += q;
                valorTotalNoCarrinho += tabelaProdPrecoVenda.get(idx) * q;
            }

            String etiquetaFiltro = filtroVitrineAtivo.isEmpty() ? "TODOS OS ITENS" : "FILTRO: '" + filtroVitrineAtivo + "'";
            System.out.println(CoresAnsi.titulo("\n--- VITRINE [" + etiquetaFiltro + "] (Página " + paginaAtual + " de " + totalPaginas + ") ---"));
            System.out.printf("🛒 CARRINHO ATUAL: %d un (%d tipos) | Subtotal: %s\n",
                    qtdTotalNoCarrinho, carrinhoIndicesProdutos.size(), CoresAnsi.sucesso(formatarValorMonetarioReais(valorTotalNoCarrinho)));
            System.out.println("----------------------------------------------------------------");

            if (totalFiltrados == 0) {
                System.out.println(CoresAnsi.aviso(" Nenhum item corresponde ao filtro '" + filtroVitrineAtivo + "'. Pressione [L] para limpar."));
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
                        leitorEntrada, sessao, auditoria, caixa, persistencia, moduloAutoridades,
                        tabelaFuncId, tabelaFuncNome, tabelaFuncCpf,
                        tabelaFuncCargo, tabelaFuncSenha,
                        tabelaFuncSalario, tabelaFuncPago,
                        tabelaFuncStatus, tabelaFuncMotivoInativo,
                        tabelaFornRazaoSocial, tabelaFornCnpj, tabelaFornUf,
                        tabelaFornProdutoPrincipal, tabelaFornValorFatura,
                        tabelaFornMargemLucroPct, tabelaFornFaturaPaga,
                        tabelaProdNome, tabelaProdNcm, tabelaProdPrecoVenda,
                        tabelaProdQtdEstoque, tabelaClienteCpf, tabelaClienteNome,
                        tabelaTransacaoTipo, tabelaTransacaoEnvolvidos, tabelaTransacaoDescricao, tabelaTransacaoValor
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
                        tabelaTransacaoTipo, tabelaTransacaoEnvolvidos, tabelaTransacaoDescricao, tabelaTransacaoValor,
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
                        tabelaTransacaoTipo, tabelaTransacaoEnvolvidos, tabelaTransacaoDescricao, tabelaTransacaoValor,
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
        Scanner leitorEntrada = new Scanner(System.in);

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
        ModuloPersistenciaTxt persistencia = new ModuloPersistenciaTxt(ARQUIVO_PRE_CADASTROS, ARQUIVO_BACKUP_OFICIAL, ARQUIVO_ROLLBACK_AUTO);

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
                        leitorEntrada, caixa, persistencia, moduloAutoridades, movEstoque,
                        tabelaFuncId, tabelaFuncNome, tabelaFuncCpf, tabelaFuncCargo, tabelaFuncSenha,
                        tabelaFuncSalario, tabelaFuncPago, tabelaFuncStatus, tabelaFuncMotivoInativo,
                        tabelaFornRazaoSocial, tabelaFornCnpj, tabelaFornUf,
                        tabelaFornProdutoPrincipal, tabelaFornValorFatura, tabelaFornMargemLucroPct, tabelaFornFaturaPaga,
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

                    // Abre "Meu Perfil" ao entrar e em seguida o painel administrativo
                    exibirAbaMeuPerfil(
                            leitorEntrada, sessao, auditoria, caixa, persistencia, moduloAutoridades,
                            tabelaFuncId, tabelaFuncNome, tabelaFuncCpf,
                            tabelaFuncCargo, tabelaFuncSenha,
                            tabelaFuncSalario, tabelaFuncPago,
                            tabelaFuncStatus, tabelaFuncMotivoInativo,
                            tabelaFornRazaoSocial, tabelaFornCnpj, tabelaFornUf,
                            tabelaFornProdutoPrincipal, tabelaFornValorFatura,
                            tabelaFornMargemLucroPct, tabelaFornFaturaPaga,
                            tabelaProdNome, tabelaProdNcm, tabelaProdPrecoVenda,
                            tabelaProdQtdEstoque, tabelaClienteCpf, tabelaClienteNome,
                            tabelaTransacaoTipo, tabelaTransacaoEnvolvidos, tabelaTransacaoDescricao, tabelaTransacaoValor
                    );

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
        // Fecha o Scanner ao encerrar o programa
        leitorEntrada.close();
    }
}