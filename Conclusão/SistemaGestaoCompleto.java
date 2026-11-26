import java.io.BufferedReader; // Usado em lerLinhasSeguras para ler linhas de arquivos de backup/TXT
import java.io.BufferedWriter; // Usado em escreverLinhas para gravar arquivos .TXT e backup
import java.io.File; // Usado em inicializarSistemaNaAbertura, lerLinhasSeguras e escreverLinhas para verificar/acessar arquivos
import java.io.FileReader; // Usado em lerLinhasSeguras com BufferedReader para leitura de arquivos
import java.io.FileWriter; // Usado em escreverLinhas com BufferedWriter para escrita de arquivos
import java.io.IOException; // Usado em enviarParaQuarentena, lerLinhasSeguras, escreverLinhas e consultarArquivoTxt para tratar falhas de I/O
import java.nio.charset.StandardCharsets; // Usado em enviarParaQuarentena, lerLinhasSeguras e consultarArquivoTxt para codificacao UTF-8
import java.nio.file.Files; // Usado em inicializarSistemaNaAbertura, enviarParaQuarentena, lerLinhasSeguras e consultarArquivoTxt para operacoes de arquivo/pasta
import java.nio.file.Path; // Usado em inicializarSistemaNaAbertura, enviarParaQuarentena, escreverLinhas e consultarArquivoTxt para caminhos de arquivos
import java.nio.file.Paths; // Usado em inicializarSistemaNaAbertura, salvarTodosArquivosTxtEBackup, gerarArquivoTesteCorrompido e consultarArquivoTxt para criar Path
import java.nio.file.StandardCopyOption; // Usado em enviarParaQuarentena para mover arquivos para a pasta de quarentena
import java.nio.file.StandardOpenOption; // Usado em enviarParaQuarentena para criar/acrescentar no log de quarentena
import java.text.NumberFormat; // Usado na constante MOEDA_PT_BR e em formatarMoeda para formatar valores em moeda
import java.time.LocalDate; // Usado em isDataValida, calcularIdade e dataHojeFormatada para manipular datas
import java.time.LocalDateTime; // Usado em dataHoraAtualFormatada para obter data e hora atuais
import java.time.Period; // Usado em calcularIdade para calcular a diferenca de anos entre datas
import java.time.format.DateTimeFormatter; // Usado em FMT_DATA e FMT_DATA_HORA para formatar e validar datas/horas
import java.time.format.DateTimeParseException; // Usado em isDataValida para capturar datas invalidas no parse
import java.util.ArrayList; // Usado nas colecoes em memoria, carrinho, montagem de linhas, leitura e validacao de backup
import java.util.Arrays; // Usado em enviarParaQuarentena e validacao de UFs para montar listas de itens
import java.util.Date; // Usado em enviarParaQuarentena para registrar o carimbo de data/hora no log
import java.util.LinkedHashSet; // Usado em validarConsistenciaBackup para armazenar IDs unicos de produtos preservando ordem
import java.util.List; // Usado nas colecoes de registros em memoria, paginacao, leitura, carrinho e validacao de backup
import java.util.Locale; // Usado em LOCALE_PT_BR, exibirListaPaginada, salvarTodosArquivosTxtEBackup, financeiro, vendas e menus
import java.util.Scanner; // Usado em todos os menus, modais, carrinho, cadastros e operacoes interativas no console
import java.util.Set; // Usado em validarConsistenciaBackup para verificar existencia de codigos de produto nas vendas


public class SistemaGestaoCompleto {

    // Nomes dos arquivos de dados, historicos, backup e teste
    private static final String ARQ_BACKUP = "backup_sistema.txt";
    private static final String ARQ_FUNCIONARIOS = "txt_funcionarios.txt";
    private static final String ARQ_CLIENTES = "txt_clientes.txt";
    private static final String ARQ_FORNECEDORES = "txt_fornecedores.txt";
    private static final String ARQ_PRODUTOS = "txt_produtos.txt";
    private static final String ARQ_FINANCEIRO = "txt_hist_financeiro.txt";
    private static final String ARQ_VENDAS = "txt_hist_vendas.txt";
    private static final String ARQ_HIST_ESTOQUE = "txt_hist_estoque.txt";
    private static final String ARQ_HIST_ACESSOS = "txt_hist_acessos.txt";
    private static final String ARQ_TESTE_CORRUPTO = "teste_cenario_sistema.txt";
    private static final String PASTA_QUARENTENA = "quarentena";

    // Marcadores de escape para evitar quebra por ';' ou quebra de linha na persistencia .TXT
    private static final String SEP = ";";
    private static final String ESC_SEP = "&#59;";
    private static final String ESC_NL = "&#10;";
    private static final String ESC_CR = "&#13;";

    // Formatadores de data/hora, moeda e UFs validas
    private static final Locale LOCALE_PT_BR = new Locale("pt", "BR");
    private static final NumberFormat MOEDA_PT_BR = NumberFormat.getCurrencyInstance(LOCALE_PT_BR);
    private static final DateTimeFormatter FMT_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final List<String> UFS_VALIDAS = Arrays.asList(
            "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS",
            "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO"
    );

    // Estado financeiro e sequenciais em memoria
    private static double saldoCaixa = 100000.00;
    private static double aliquotaAnoAtual = 7.0;
    private static double aliquotaProximoAno = 8.5;
    private static int proxNf = 1001;
    private static int proxFin = 2;
    private static int proxMovEst = 2;
    private static int proxAcesso = 2;
    private static int proxFunc = 2;
    private static int proxCli = 2;
    private static int proxFor = 2;
    private static int proxPrd = 2;

    // Colecoes em memoria (cada linha e um registro serializado com campos escapados por ';')
    private static final List<String> funcionarios = new ArrayList<>();
    private static final List<String> clientes = new ArrayList<>();
    private static final List<String> fornecedores = new ArrayList<>();
    private static final List<String> produtos = new ArrayList<>();
    private static final List<String> histFinanceiro = new ArrayList<>();
    private static final List<String> histVendas = new ArrayList<>();
    private static final List<String> histEstoque = new ArrayList<>();
    private static final List<String> histAcessos = new ArrayList<>();

    // Carrinho do cliente: cada item no formato "codPrd;nomePrd;forId;qtd;precoUnit;custoUnit;aliq"
    private static final List<String> carrinhoCliente = new ArrayList<>();

    // Sessao ativa do funcionario (null quando deslogado / modo inicio ou cliente)
    private static String usuarioLogadoId = null;
    private static String usuarioLogadoNome = null;
    private static String usuarioLogadoCargo = null;

    public static void main(String[] args) {
        System.out.println("=== INICIALIZANDO SISTEMA UNIFICADO DE GESTAO E VENDAS ===");
        inicializarSistemaNaAbertura();
        executarLoopInicio();
    }

    // =========================================================================
    // 1. SERIALIZACAO, ESCAPE E UTILITARIOS DE DATA / MOEDA / VALIDACAO
    // =========================================================================

    /**
     * Escapa ';', '\r' e '\n' antes de gravar um campo em arquivo .TXT.
     */
    public static String escaparCampo(String valor) {
        if (valor == null) return "";
        return valor
                .replace(";", ESC_SEP)
                .replace("\r", ESC_CR)
                .replace("\n", ESC_NL);
    }

    /**
     * Restaura ';', '\r' e '\n' apos ler um campo de arquivo .TXT.
     */
    public static String desescaparCampo(String valor) {
        if (valor == null) return "";
        return valor
                .replace(ESC_CR, "\r")
                .replace(ESC_NL, "\n")
                .replace(ESC_SEP, ";");
    }

    /**
     * Monta uma linha serializada separada por ';'.
     */
    public static String montarLinha(String... campos) {
        List<String> escapados = new ArrayList<>(campos.length);
        for (String c : campos) {
            escapados.add(escaparCampo(c));
        }
        return String.join(SEP, escapados);
    }

    /**
     * Separa uma linha em campos desescapados preservando campos vazios finais.
     */
    public static String[] partirLinha(String linha) {
        String[] brutos = (linha == null ? "" : linha).split(SEP, -1);
        for (int i = 0; i < brutos.length; i++) {
            brutos[i] = desescaparCampo(brutos[i]);
        }
        return brutos;
    }

    /**
     * Valida se uma data no formato dd/MM/yyyy existe no calendario.
     */
    public static boolean isDataValida(String dataStr) {
        if (dataStr == null || dataStr.trim().isEmpty()) return false;
        try {
            LocalDate.parse(dataStr.trim(), FMT_DATA);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * Calcula a idade completa em anos a partir de uma data de nascimento dd/MM/yyyy.
     * Retorna -1 se a data for invalida ou futura.
     */
    public static int calcularIdade(String dataNascimentoStr) {
        if (!isDataValida(dataNascimentoStr)) return -1;
        LocalDate nasc = LocalDate.parse(dataNascimentoStr.trim(), FMT_DATA);
        LocalDate hoje = LocalDate.now();
        if (nasc.isAfter(hoje)) return -1;
        return Period.between(nasc, hoje).getYears();
    }

    public static String dataHojeFormatada() {
        return LocalDate.now().format(FMT_DATA);
    }

    public static String dataHoraAtualFormatada() {
        return LocalDateTime.now().format(FMT_DATA_HORA);
    }

    public static String formatarMoeda(double valor) {
        return MOEDA_PT_BR.format(valor);
    }

    /**
     * Trata divisao segura evitando ArithmeticException / NaN / Infinity.
     */
    public static double dividirSeguro(double dividendo, double divisor) {
        try {
            if (Math.abs(divisor) < 0.0000001) {
                throw new ArithmeticException("Divisao por zero bloqueada");
            }
            return dividendo / divisor;
        } catch (ArithmeticException e) {
            System.out.println("[AVISO TRATAMENTO DE EXCECAO] " + e.getMessage() + ". Retornando 0.0.");
            return 0.0;
        }
    }

    public static boolean isCpfValido(String cpf) {
        if (cpf == null) return false;
        String digitos = cpf.replaceAll("\\D", "");
        return digitos.length() == 11;
    }

    public static boolean isCnpjValido(String cnpj) {
        if (cnpj == null) return false;
        String digitos = cnpj.replaceAll("\\D", "");
        return digitos.length() == 14;
    }

    public static boolean isUfValida(String uf) {
        if (uf == null) return false;
        return UFS_VALIDAS.contains(uf.trim().toUpperCase(Locale.ROOT));
    }

    public static String normalizarTextoObrigatorio(Scanner sc, String rotulo, boolean permitirCancelar) {
        while (true) {
            System.out.print(rotulo + (permitirCancelar ? " (ou 'CANCELAR' / 'VOLTAR'): " : ": "));
            String entrada = sc.nextLine().trim();
            if (permitirCancelar && ("CANCELAR".equalsIgnoreCase(entrada) || "VOLTAR".equalsIgnoreCase(entrada))) {
                return null;
            }
            if (!entrada.isEmpty()) {
                return entrada;
            }
            System.out.println("[AVISO] Entrada invalida: este campo nao pode ficar vazio.");
        }
    }

    public static Integer lerInteiroSeguro(Scanner sc, String rotulo, Integer min, Integer max, boolean permitirCancelar) {
        while (true) {
            System.out.print(rotulo + (permitirCancelar ? " (ou 'VOLTAR'): " : ": "));
            String txt = sc.nextLine().trim();
            if (permitirCancelar && ("VOLTAR".equalsIgnoreCase(txt) || "CANCELAR".equalsIgnoreCase(txt))) {
                return null;
            }
            try {
                int val = Integer.parseInt(txt);
                if (min != null && val < min) {
                    System.out.println("[AVISO] Valor deve ser maior ou igual a " + min + ".");
                    continue;
                }
                if (max != null && val > max) {
                    System.out.println("[AVISO] Valor deve ser menor ou igual a " + max + ".");
                    continue;
                }
                return val;
            } catch (NumberFormatException e) {
                System.out.println("[AVISO TRATAMENTO DE EXCECAO] Entrada invalida: digite um numero inteiro.");
            }
        }
    }

    public static Double lerDecimalSeguro(Scanner sc, String rotulo, Double min, boolean permitirCancelar) {
        while (true) {
            System.out.print(rotulo + (permitirCancelar ? " (ou 'VOLTAR'): " : ": "));
            String txt = sc.nextLine().trim().replace(",", ".");
            if (permitirCancelar && ("VOLTAR".equalsIgnoreCase(txt) || "CANCELAR".equalsIgnoreCase(txt))) {
                return null;
            }
            try {
                double val = Double.parseDouble(txt);
                if (Double.isNaN(val) || Double.isInfinite(val)) {
                    System.out.println("[AVISO] Numero decimal invalido.");
                    continue;
                }
                if (min != null && val < min) {
                    System.out.println("[AVISO] Valor deve ser maior ou igual a " + min + ".");
                    continue;
                }
                return Math.round(val * 100.0) / 100.0;
            } catch (NumberFormatException e) {
                System.out.println("[AVISO TRATAMENTO DE EXCECAO] Entrada invalida: digite um numero valido.");
            }
        }
    }

    /**
     * Busca segura por chave/ID em uma coluna de uma lista de registros.
     */
    public static int encontrarIndicePorChave(List<String> lista, int colunaChave, String chaveBuscada) {
        if (lista == null || chaveBuscada == null || colunaChave < 0) return -1;
        String alvo = chaveBuscada.trim();
        for (int i = 0; i < lista.size(); i++) {
            try {
                String[] campos = partirLinha(lista.get(i));
                if (campos.length > colunaChave && campos[colunaChave].trim().equalsIgnoreCase(alvo)) {
                    return i;
                }
            } catch (IndexOutOfBoundsException e) {
                System.out.println("[AVISO TRATAMENTO DE EXCECAO] Acesso indevido a indice na linha " + i);
            }
        }
        return -1;
    }

    // =========================================================================
    // 2. INICIALIZACAO, VALIDACAO DE BACKUP, QUARENTENA E PERSISTENCIA .TXT
    // =========================================================================

    /**
     * Verifica o backup na abertura: se houver inconsistencia real, move para quarentena,
     * avisa o usuario e carrega base padrao limpa.
     */
    public static void inicializarSistemaNaAbertura() {
        Path pathBackup = Paths.get(ARQ_BACKUP);
        File arquivoBackup = pathBackup.toFile();

        if (!arquivoBackup.exists() || !Files.exists(pathBackup)) {
            System.out.println("[AVISO] Arquivo " + ARQ_BACKUP + " nao encontrado. Gerando base padrao.");
            carregarBasePadrao();
            registrarAcesso("SISTEMA_INICIO", "BACKUP_AUSENTE_BASE_PADRAO", "OK");
            salvarTodosArquivosTxtEBackup();
            return;
        }

        List<String> linhasBackup = lerLinhasSeguras(pathBackup);
        String motivoInconsistencia = validarConsistenciaBackup(linhasBackup);

        if (motivoInconsistencia != null) {
            enviarParaQuarentena(pathBackup, motivoInconsistencia);
            carregarBasePadrao();
            registrarAcesso("QUARENTENA_BACKUP", motivoInconsistencia, "ALERTA");
            salvarTodosArquivosTxtEBackup();
            System.out.println("[ALERTA] Backup inconsistente detectado (" + motivoInconsistencia + ").");
            System.out.println("[ALERTA] Backup e .TXT afetados movidos para '" + PASTA_QUARENTENA + "/'.");
            System.out.println("[INFO] Sistema aberto sem dados inconsistentes da base anterior (base padrao ativa).");
            return;
        }

        carregarDadosDoBackup(linhasBackup);
        sincronizarArquivosTxtComMemoria();
        registrarAcesso("LOGIN_BACKUP_OK", "DADOS_CARREGADOS_E_SINCRONIZADOS", "OK");
        System.out.println("[OK] Backup validado e todos os .TXT sincronizados com sucesso.");
    }

    /**
     * Valida secoes obrigatorias, colunas, tipos, referencia de produtos nas vendas e saldo final.
     */
    public static String validarConsistenciaBackup(List<String> linhas) {
        if (linhas == null || linhas.isEmpty()) {
            return "Arquivo de backup vazio";
        }

        boolean temControle = false;
        boolean temFuncionarios = false;
        boolean temClientes = false;
        boolean temFornecedores = false;
        boolean temProdutos = false;
        boolean temFinanceiro = false;
        boolean temVendas = false;
        boolean temHistEstoque = false;
        boolean temHistAcessos = false;

        Set<String> idsProdutos = new LinkedHashSet<>();
        List<String> linhasVendas = new ArrayList<>();
        Double saldoControle = null;
        Double ultimoSaldoFinanceiro = null;

        String secaoAtual = "";
        for (int i = 0; i < linhas.size(); i++) {
            String bruta = linhas.get(i);
            if (bruta == null) continue;
            String trim = bruta.trim();
            if (trim.isEmpty()) continue;

            if (trim.startsWith("[") && trim.endsWith("]")) {
                secaoAtual = trim;
                if ("[CONTROLE]".equals(secaoAtual)) temControle = true;
                else if ("[FUNCIONARIOS]".equals(secaoAtual)) temFuncionarios = true;
                else if ("[CLIENTES]".equals(secaoAtual)) temClientes = true;
                else if ("[FORNECEDORES]".equals(secaoAtual)) temFornecedores = true;
                else if ("[PRODUTOS]".equals(secaoAtual)) temProdutos = true;
                else if ("[FINANCEIRO]".equals(secaoAtual)) temFinanceiro = true;
                else if ("[VENDAS]".equals(secaoAtual)) temVendas = true;
                else if ("[HIST_ESTOQUE]".equals(secaoAtual)) temHistEstoque = true;
                else if ("[HIST_ACESSOS]".equals(secaoAtual)) temHistAcessos = true;
                continue;
            }

            if (secaoAtual.isEmpty()) {
                return "Conteudo fora de secao na linha " + (i + 1);
            }

            String[] partes = partirLinha(bruta);
            switch (secaoAtual) {
                case "[CONTROLE]":
                    if (partes.length < 2) return "Linha invalida em [CONTROLE]: " + (i + 1);
                    if ("SALDO".equals(partes[0])) {
                        try {
                            saldoControle = Double.parseDouble(partes[1].trim());
                        } catch (NumberFormatException e) {
                            return "SALDO invalido em [CONTROLE]";
                        }
                    } else if ("PROX_NF".equals(partes[0])) {
                        try {
                            int pn = Integer.parseInt(partes[1].trim());
                            if (pn <= 0) return "PROX_NF invalido em [CONTROLE]";
                        } catch (NumberFormatException e) {
                            return "PROX_NF invalido em [CONTROLE]";
                        }
                    } else if ("ALIQ_ATUAL".equals(partes[0])) {
                        try {
                            Double.parseDouble(partes[1].trim());
                        } catch (NumberFormatException e) {
                            return "ALIQ_ATUAL invalida em [CONTROLE]";
                        }
                    } else if ("ALIQ_PROX".equals(partes[0])) {
                        try {
                            Double.parseDouble(partes[1].trim());
                        } catch (NumberFormatException e) {
                            return "ALIQ_PROX invalida em [CONTROLE]";
                        }
                    }
                    break;
                case "[FUNCIONARIOS]":
                    if (partes.length < 14 || partes[0].trim().isEmpty()) {
                        return "Registro invalido em [FUNCIONARIOS] na linha " + (i + 1);
                    }
                    break;
                case "[CLIENTES]":
                    if (partes.length < 3 || partes[0].trim().isEmpty()) {
                        return "Registro invalido em [CLIENTES] na linha " + (i + 1);
                    }
                    break;
                case "[FORNECEDORES]":
                    if (partes.length < 6 || partes[0].trim().isEmpty()) {
                        return "Registro invalido em [FORNECEDORES] na linha " + (i + 1);
                    }
                    break;
                case "[PRODUTOS]":
                    if (partes.length < 9 || partes[0].trim().isEmpty()) {
                        return "Registro invalido em [PRODUTOS] na linha " + (i + 1);
                    }
                    try {
                        double custo = Double.parseDouble(partes[3].trim());
                        double preco = Double.parseDouble(partes[4].trim());
                        int estoque = Integer.parseInt(partes[5].trim());
                        if (custo < 0 || preco < 0 || estoque < 0) {
                            return "Valores negativos em [PRODUTOS] (" + partes[0] + ")";
                        }
                    } catch (NumberFormatException e) {
                        return "Numero invalido em [PRODUTOS] (" + partes[0] + ")";
                    }
                    idsProdutos.add(partes[0].trim());
                    break;
                case "[FINANCEIRO]":
                    if (partes.length < 6 || partes[0].trim().isEmpty()) {
                        return "Registro invalido em [FINANCEIRO] na linha " + (i + 1);
                    }
                    try {
                        Double.parseDouble(partes[4].trim());
                        ultimoSaldoFinanceiro = Double.parseDouble(partes[5].trim());
                    } catch (NumberFormatException e) {
                        return "Valor financeiro invalido em [FINANCEIRO] (" + partes[0] + ")";
                    }
                    break;
                case "[VENDAS]":
                    if (partes.length < 10 || partes[0].trim().isEmpty()) {
                        return "Registro invalido em [VENDAS] na linha " + (i + 1);
                    }
                    try {
                        double totalNf = Double.parseDouble(partes[9].trim());
                        if (totalNf < 0) return "Total de NF negativo em " + partes[0];
                    } catch (NumberFormatException e) {
                        return "Valor numerico invalido em [VENDAS] (" + partes[0] + ")";
                    }
                    linhasVendas.add(bruta);
                    break;
                case "[HIST_ESTOQUE]":
                    if (partes.length < 9 || partes[0].trim().isEmpty()) {
                        return "Registro invalido em [HIST_ESTOQUE] na linha " + (i + 1);
                    }
                    break;
                case "[HIST_ACESSOS]":
                    if (partes.length < 7 || partes[0].trim().isEmpty()) {
                        return "Registro invalido em [HIST_ACESSOS] na linha " + (i + 1);
                    }
                    break;
                default:
                    break;
            }
        }

        if (!temControle || !temFuncionarios || !temClientes || !temFornecedores || !temProdutos
                || !temFinanceiro || !temVendas || !temHistEstoque || !temHistAcessos) {
            return "Uma ou mais secoes obrigatorias estao ausentes no backup";
        }

        if (!idsProdutos.isEmpty()) {
            for (String vLine : linhasVendas) {
                String[] vp = partirLinha(vLine);
                String itemQtd = vp[4].trim();
                int posX = itemQtd.lastIndexOf('x');
                String codPrd = posX > 0 ? itemQtd.substring(0, posX).trim() : itemQtd;
                if (!idsProdutos.contains(codPrd)) {
                    return "Venda " + vp[0] + " referencia produto inexistente " + codPrd;
                }
            }
        }

        if (saldoControle != null && ultimoSaldoFinanceiro != null
                && Math.abs(saldoControle - ultimoSaldoFinanceiro) > 0.05) {
            return "Divergencia entre SALDO de controle (" + saldoControle
                    + ") e saldo final de [FINANCEIRO] (" + ultimoSaldoFinanceiro + ")";
        }

        return null;
    }

    /**
     * Carrega os dados validados do backup para as listas em memoria.
     */
    public static void carregarDadosDoBackup(List<String> linhas) {
        funcionarios.clear();
        clientes.clear();
        fornecedores.clear();
        produtos.clear();
        histFinanceiro.clear();
        histVendas.clear();
        histEstoque.clear();
        histAcessos.clear();

        String secao = "";
        for (String raw : linhas) {
            if (raw == null) continue;
            String trim = raw.trim();
            if (trim.isEmpty()) continue;
            if (trim.startsWith("[") && trim.endsWith("]")) {
                secao = trim;
                continue;
            }
            switch (secao) {
                case "[CONTROLE]":
                    String[] p = partirLinha(raw);
                    if (p.length >= 2 && "SALDO".equals(p[0].trim())) {
                        saldoCaixa = Double.parseDouble(p[1].trim());
                    } else if (p.length >= 2 && "PROX_NF".equals(p[0].trim())) {
                        proxNf = Integer.parseInt(p[1].trim());
                    } else if (p.length >= 2 && "ALIQ_ATUAL".equals(p[0].trim())) {
                        aliquotaAnoAtual = Double.parseDouble(p[1].trim());
                    } else if (p.length >= 2 && "ALIQ_PROX".equals(p[0].trim())) {
                        aliquotaProximoAno = Double.parseDouble(p[1].trim());
                    }
                    break;
                case "[FUNCIONARIOS]":
                    funcionarios.add(raw);
                    proxFunc = funcionarios.size() + 1;
                    break;
                case "[CLIENTES]":
                    clientes.add(raw);
                    proxCli = clientes.size() + 1;
                    break;
                case "[FORNECEDORES]":
                    fornecedores.add(raw);
                    proxFor = fornecedores.size() + 1;
                    break;
                case "[PRODUTOS]":
                    produtos.add(raw);
                    proxPrd = produtos.size() + 1;
                    break;
                case "[FINANCEIRO]":
                    histFinanceiro.add(raw);
                    proxFin = histFinanceiro.size() + 1;
                    break;
                case "[VENDAS]":
                    histVendas.add(raw);
                    break;
                case "[HIST_ESTOQUE]":
                    histEstoque.add(raw);
                    proxMovEst = histEstoque.size() + 1;
                    break;
                case "[HIST_ACESSOS]":
                    histAcessos.add(raw);
                    proxAcesso = histAcessos.size() + 1;
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Move o backup inconsistente e os .TXT relacionados para a pasta de quarentena e registra o motivo.
     */
    public static void enviarParaQuarentena(Path pathBackup, String motivo) {
        try {
            Path dirQuarentena = Paths.get(PASTA_QUARENTENA);
            Files.createDirectories(dirQuarentena);
            String sufixo = "_quarentena_" + System.currentTimeMillis() + ".txt";

            if (Files.exists(pathBackup)) {
                Files.move(pathBackup, dirQuarentena.resolve(ARQ_BACKUP + sufixo), StandardCopyOption.REPLACE_EXISTING);
            }

            List<String> arquivosTxt = Arrays.asList(
                    ARQ_FUNCIONARIOS, ARQ_CLIENTES, ARQ_FORNECEDORES, ARQ_PRODUTOS,
                    ARQ_FINANCEIRO, ARQ_VENDAS, ARQ_HIST_ESTOQUE, ARQ_HIST_ACESSOS
            );
            for (String nomeTxt : arquivosTxt) {
                Path p = Paths.get(nomeTxt);
                if (Files.exists(p)) {
                    Files.move(p, dirQuarentena.resolve(nomeTxt + sufixo), StandardCopyOption.REPLACE_EXISTING);
                }
            }

            Path logQuarentena = dirQuarentena.resolve("motivo_quarentena.log");
            String registro = "DataHora=" + new Date() + " (" + dataHoraAtualFormatada() + ") | Motivo="
                    + motivo + System.lineSeparator();
            Files.write(logQuarentena, registro.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.out.println("[ERRO] Falha ao mover arquivos para quarentena: " + e.getMessage());
        }
    }

    /**
     * Popula uma base padrao limpa e consistente.
     */
    public static void carregarBasePadrao() {
        saldoCaixa = 100000.00;
        aliquotaAnoAtual = 7.0;
        aliquotaProximoAno = 8.5;
        proxNf = 1001;
        proxFin = 2;
        proxMovEst = 2;
        proxAcesso = 2;
        proxFunc = 2;
        proxCli = 2;
        proxFor = 2;
        proxPrd = 2;

        // Campos funcionario:
        // 0:id | 1:nome | 2:cpf | 3:nascimento | 4:sexo | 5:cargo | 6:senha | 7:salario |
        // 8:bloqueado | 9:ativo | 10:pago | 11:tentativasErro | 12:motivoDemissao | 13:autoridade
        funcionarios.clear();
        funcionarios.add(montarLinha("UR-FUNC01", "FUNCIONARIO TESTE 01", "11144477735", "02/02/1981",
                "F", "GERENCIA", "senha01", "3550.00", "false", "true", "false", "0", "N/A", "GERENCIA"));

        // Campos cliente: 0:id | 1:cpf | 2:nome
        clientes.clear();
        clientes.add(montarLinha("CLI-001", "11122233301", "CLIENTE GESTAO 001"));

        // Campos fornecedor: 0:id | 1:nome | 2:cnpj | 3:uf | 4:codPrdPrincipal | 5:custoUnit
        fornecedores.clear();
        fornecedores.add(montarLinha("FOR-001", "FORNECEDOR PARCEIRO 001 LTDA", "10000000123456",
                "SP", "PRD-001", "500.00"));

        // Campos produto: 0:id | 1:nome | 2:forId | 3:custoUnit | 4:precoUnit | 5:estoque | 6:aliqAtual | 7:aliqProx | 8:ativo
        produtos.clear();
        produtos.add(montarLinha("PRD-001", "PRODUTO GESTAO ITEM 001", "FOR-001",
                "11.45", "17.96", "21", "7.0", "8.5", "true"));

        // Campos financeiro: 0:id | 1:data | 2:tipo | 3:descricao | 4:valor | 5:saldoApos
        histFinanceiro.clear();
        histFinanceiro.add(montarLinha("FIN-0001", dataHojeFormatada(), "DEPOSITO_INICIAL",
                "Abertura de caixa inicial", "100000.00", "100000.00"));

        histVendas.clear();

        // Campos estoque: 0:id | 1:data | 2:tipo | 3:prdId | 4:nomePrd | 5:forId | 6:qtd | 7:usuarioId | 8:obs
        histEstoque.clear();
        histEstoque.add(montarLinha("MOV-0001", dataHojeFormatada(), "ENTRADA", "PRD-001",
                "PRODUTO GESTAO ITEM 001", "FOR-001", "21", "UR-FUNC01", "Carga inicial base padrao"));

        // Campos acessos: 0:id | 1:dataHora | 2:usuarioId | 3:cargo | 4:evento | 5:detalhe | 6:status
        histAcessos.clear();
        histAcessos.add(montarLinha("ACC-0001", dataHoraAtualFormatada(), "UR-FUNC01",
                "GERENCIA", "LOGIN_SUCESSO", "MENU_INICIAL", "OK"));
    }

    /**
     * Grava todos os arquivos .TXT individuais e o backup_sistema.txt consolidado.
     */
    public static void salvarTodosArquivosTxtEBackup() {
        escreverLinhas(Paths.get(ARQ_FUNCIONARIOS), funcionarios);
        escreverLinhas(Paths.get(ARQ_CLIENTES), clientes);
        escreverLinhas(Paths.get(ARQ_FORNECEDORES), fornecedores);
        escreverLinhas(Paths.get(ARQ_PRODUTOS), produtos);
        escreverLinhas(Paths.get(ARQ_FINANCEIRO), histFinanceiro);
        escreverLinhas(Paths.get(ARQ_VENDAS), histVendas);
        escreverLinhas(Paths.get(ARQ_HIST_ESTOQUE), histEstoque);
        escreverLinhas(Paths.get(ARQ_HIST_ACESSOS), histAcessos);

        List<String> backupLinhas = new ArrayList<>();
        backupLinhas.add("[CONTROLE]");
        backupLinhas.add(montarLinha("SALDO", String.format(Locale.US, "%.2f", saldoCaixa)));
        backupLinhas.add(montarLinha("PROX_NF", String.valueOf(proxNf)));
        backupLinhas.add(montarLinha("ALIQ_ATUAL", String.format(Locale.US, "%.2f", aliquotaAnoAtual)));
        backupLinhas.add(montarLinha("ALIQ_PROX", String.format(Locale.US, "%.2f", aliquotaProximoAno)));

        adicionarSecao(backupLinhas, "[FUNCIONARIOS]", funcionarios);
        adicionarSecao(backupLinhas, "[CLIENTES]", clientes);
        adicionarSecao(backupLinhas, "[FORNECEDORES]", fornecedores);
        adicionarSecao(backupLinhas, "[PRODUTOS]", produtos);
        adicionarSecao(backupLinhas, "[FINANCEIRO]", histFinanceiro);
        adicionarSecao(backupLinhas, "[VENDAS]", histVendas);
        adicionarSecao(backupLinhas, "[HIST_ESTOQUE]", histEstoque);
        adicionarSecao(backupLinhas, "[HIST_ACESSOS]", histAcessos);

        escreverLinhas(Paths.get(ARQ_BACKUP), backupLinhas);
    }

    public static void sincronizarArquivosTxtComMemoria() {
        salvarTodosArquivosTxtEBackup();
    }

    private static void adicionarSecao(List<String> destino, String cabecalho, List<String> itens) {
        destino.add(cabecalho);
        destino.addAll(itens);
    }

    private static List<String> lerLinhasSeguras(Path caminho) {
        List<String> linhas = new ArrayList<>();
        File arq = caminho.toFile();
        if (!arq.exists()) return linhas;
        try (BufferedReader br = new BufferedReader(new FileReader(arq))) {
            String linha;
            while ((linha = br.readLine()) != null) {
                linhas.add(linha);
            }
        } catch (IOException e) {
            try {
                return Files.readAllLines(caminho, StandardCharsets.UTF_8);
            } catch (IOException ignored) {
                return new ArrayList<>();
            }
        }
        return linhas;
    }

    private static void escreverLinhas(Path caminho, List<String> linhas) {
        File arq = caminho.toFile();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(arq))) {
            for (int i = 0; i < linhas.size(); i++) {
                bw.write(linhas.get(i));
                if (i < linhas.size() - 1) {
                    bw.newLine();
                }
            }
        } catch (IOException e) {
            System.out.println("[ERRO] Falha ao gravar " + caminho.getFileName() + ": " + e.getMessage());
        }
    }

    // =========================================================================
    // 3. PAGINACAO SEGURA, HISTORICOS E AUDITORIA
    // =========================================================================

    /**
     * Paginacao segura com limite de paginas (ex.: ate 25 paginas), sem IndexOutOfBounds.
     */
    public static void exibirListaPaginada(Scanner sc, String titulo, List<String> dados, int itensPorPagina, int maxPaginas) {
        if (dados == null || dados.isEmpty()) {
            System.out.println("\n[" + titulo + "] Nenhum registro encontrado.");
            return;
        }
        int tamanhoPagina = Math.max(1, itensPorPagina);
        int paginasCalculadas = (dados.size() + tamanhoPagina - 1) / tamanhoPagina;
        int totalPaginas = maxPaginas > 0 ? Math.min(paginasCalculadas, maxPaginas) : paginasCalculadas;
        int limiteItensExibiveis = Math.min(dados.size(), totalPaginas * tamanhoPagina);
        int paginaAtual = 1;

        while (true) {
            paginaAtual = Math.max(1, Math.min(paginaAtual, totalPaginas));
            int inicio = (paginaAtual - 1) * tamanhoPagina;
            int fim = Math.min(inicio + tamanhoPagina, limiteItensExibiveis);

            System.out.println("\n=== " + titulo + " (Pagina " + paginaAtual + " de " + totalPaginas
                    + " | Exibidos: " + limiteItensExibiveis + " de " + dados.size() + ") ===");
            for (int i = inicio; i < fim; i++) {
                System.out.println((i + 1) + ". " + String.join(" | ", partirLinha(dados.get(i))));
            }

            System.out.print("[N] Proxima | [A] Anterior | [P] Ir para pagina | [S] Sair: ");
            String cmd = sc.nextLine().trim().toUpperCase(Locale.ROOT);
            if ("N".equals(cmd)) {
                if (paginaAtual < totalPaginas) {
                    paginaAtual++;
                } else {
                    System.out.println("Voce ja esta na ultima pagina.");
                }
            } else if ("A".equals(cmd)) {
                if (paginaAtual > 1) {
                    paginaAtual--;
                } else {
                    System.out.println("Voce ja esta na primeira pagina.");
                }
            } else if ("P".equals(cmd)) {
                System.out.print("Numero da pagina (1 a " + totalPaginas + "): ");
                try {
                    int escolhida = Integer.parseInt(sc.nextLine().trim());
                    if (escolhida >= 1 && escolhida <= totalPaginas) {
                        paginaAtual = escolhida;
                    } else {
                        System.out.println("Pagina fora do intervalo valido.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Numero de pagina invalido.");
                }
            } else if ("S".equals(cmd) || cmd.isEmpty()) {
                break;
            } else {
                System.out.println("Comando invalido.");
            }
        }
    }

    public static void registrarAcesso(String evento, String detalhe, String status) {
        String id = String.format("ACC-%04d", proxAcesso++);
        String atorId = usuarioLogadoId != null ? usuarioLogadoId : "VISITANTE/SISTEMA";
        String atorCargo = usuarioLogadoCargo != null ? usuarioLogadoCargo : "PUBLICO";
        histAcessos.add(montarLinha(id, dataHoraAtualFormatada(), atorId, atorCargo, evento, detalhe, status));
    }

    public static void registrarMovEstoque(String tipoMov, String prdId, String nomePrd, String forId, int qtd, String obs) {
        String id = String.format("MOV-%04d", proxMovEst++);
        String atorId = usuarioLogadoId != null ? usuarioLogadoId : "PORTAL_CLIENTE";
        histEstoque.add(montarLinha(id, dataHojeFormatada(), tipoMov, prdId, nomePrd,
                forId, String.valueOf(qtd), atorId, obs));
    }

    public static void registrarLancamentoFinanceiro(String tipo, String descricao, double valor) {
        saldoCaixa = Math.round((saldoCaixa + valor) * 100.0) / 100.0;
        String id = String.format(Locale.US, "FIN-%04d", proxFin++);
        histFinanceiro.add(montarLinha(id, dataHojeFormatada(), tipo, descricao,
                String.format(Locale.US, "%.2f", valor),
                String.format(Locale.US, "%.2f", saldoCaixa)));
    }

    /**
     * Gera arquivo .TXT de teste para simular cenarios na abertura.
     */
    public static void gerarArquivoTesteCorrompido() {
        List<String> corrompido = new ArrayList<>();
        corrompido.add("[CONTROLE]");
        corrompido.add(montarLinha("SALDO", "999999.00"));
        corrompido.add(montarLinha("PROX_NF", "1001"));
        corrompido.add("[FUNCIONARIOS]");
        corrompido.addAll(funcionarios);
        corrompido.add("[CLIENTES]");
        corrompido.addAll(clientes);
        corrompido.add("[FORNECEDORES]");
        corrompido.addAll(fornecedores);
        corrompido.add("[PRODUTOS]");
        corrompido.addAll(produtos);
        corrompido.add("[FINANCEIRO]");
        corrompido.addAll(histFinanceiro);
        corrompido.add("[VENDAS]");
        corrompido.add(montarLinha("NF-9999", dataHojeFormatada(), "11122233301", "CLIENTE GESTAO 001",
                "PRD-999x2", "100.00", "5.00", "50.00", "45.00", "105.00"));
        corrompido.add("[HIST_ESTOQUE]");
        corrompido.addAll(histEstoque);
        corrompido.add("[HIST_ACESSOS]");
        corrompido.addAll(histAcessos);

        escreverLinhas(Paths.get(ARQ_TESTE_CORRUPTO), corrompido);
        System.out.println("[OK] Arquivo de teste gerado: " + ARQ_TESTE_CORRUPTO);
        System.out.println("Para testar a quarentena na abertura, renomeie/copie '" + ARQ_TESTE_CORRUPTO
                + "' para '" + ARQ_BACKUP + "' e execute novamente.");
    }

    // =========================================================================
    // 4. NAVEGACAO INICIAL, LOGIN E PERMISSOES
    // =========================================================================

    public static void executarLoopInicio() {
        Scanner sc = new Scanner(System.in);
        boolean rodando = true;

        while (rodando) {
            deslogarSessaoSilenciosa();
            System.out.println("\n================ TELA INICIAL ================");
            System.out.println("1 - Acessar como Cliente (Vitrine / Carrinho / Compra)");
            System.out.println("2 - Login de Funcionario / Gestao / RH / Estoque / Caixa / Financeiro / TI");
            System.out.println("3 - Salvar backup manual (.TXT) e sincronizar arquivos");
            System.out.println("4 - Gerar arquivo .TXT de teste (simular inconsistencia)");
            System.out.println("0 - Encerrar sistema (salva backup automatico)");
            System.out.print("Escolha: ");

            String op = sc.nextLine().trim();
            switch (op) {
                case "1":
                    menuCliente(sc);
                    break;
                case "2":
                    if (realizarLoginFuncionario(sc)) {
                        menuFuncionarioAutenticado(sc);
                    }
                    break;
                case "3":
                    salvarTodosArquivosTxtEBackup();
                    registrarAcesso("BACKUP_MANUAL_INICIO", "SINCRONIZACAO_TXT_E_BACKUP", "OK");
                    salvarTodosArquivosTxtEBackup();
                    System.out.println("[OK] Backup e arquivos .TXT salvos com sucesso.");
                    break;
                case "4":
                    gerarArquivoTesteCorrompido();
                    break;
                case "0":
                    salvarTodosArquivosTxtEBackup();
                    registrarAcesso("ENCERRAR_SISTEMA", "BACKUP_AUTOMATICO_NO_FECHAMENTO", "OK");
                    salvarTodosArquivosTxtEBackup();
                    System.out.println("[OK] Backup automatico concluido. Ate logo!");
                    rodando = false;
                    break;
                default:
                    System.out.println("[AVISO] Opcao invalida.");
                    break;
            }
        }
        sc.close();
    }

    private static void deslogarSessaoSilenciosa() {
        usuarioLogadoId = null;
        usuarioLogadoNome = null;
        usuarioLogadoCargo = null;
    }

    private static void voltarAoInicioDeslogando(String motivo) {
        if (usuarioLogadoId != null) {
            registrarAcesso("VOLTAR_INICIO_LOGOUT", "Usuario " + usuarioLogadoId + " deslogado (" + motivo + ")", "OK");
            salvarTodosArquivosTxtEBackup();
        }
        deslogarSessaoSilenciosa();
    }

    private static boolean realizarLoginFuncionario(Scanner sc) {
        System.out.println("\n--- LOGIN DE FUNCIONARIO ---");
        System.out.print("ID ou CPF do funcionario (ou 'VOLTAR'): ");
        String identificador = sc.nextLine().trim();
        if ("VOLTAR".equalsIgnoreCase(identificador) || identificador.isEmpty()) {
            return false;
        }

        int idx = encontrarIndicePorChave(funcionarios, 0, identificador);
        if (idx == -1) {
            idx = encontrarIndicePorChave(funcionarios, 2, identificador.replaceAll("\\D", ""));
        }
        if (idx == -1) {
            System.out.println("[AVISO] Funcionario nao encontrado. E necessario cadastro previo.");
            registrarAcesso("LOGIN_FALHA", "Identificador inexistente: " + identificador, "NEGADO");
            salvarTodosArquivosTxtEBackup();
            return false;
        }

        String[] f = partirLinha(funcionarios.get(idx));
        String idFunc = f[0];
        String nomeFunc = f[1];
        String cargoFunc = f[5];
        String senhaCorreta = f[6];
        boolean bloqueado = Boolean.parseBoolean(f[8]);
        boolean ativo = Boolean.parseBoolean(f[9]);
        int tentativasErro = 0;
        try {
            tentativasErro = Integer.parseInt(f[11]);
        } catch (NumberFormatException ignored) {
        }
        String autoridade = f.length > 13 ? f[13] : cargoFunc;

        if (!ativo) {
            System.out.println("[AVISO] Funcionario desativado/demitido. Autoridade revogada.");
            registrarAcesso("LOGIN_INATIVO", idFunc + " (" + nomeFunc + ")", "NEGADO");
            salvarTodosArquivosTxtEBackup();
            return false;
        }

        if (bloqueado) {
            System.out.println("[BLOQUEADO] Login bloqueado por excesso de tentativas. Solicite desbloqueio a TI.");
            registrarAcesso("LOGIN_BLOQUEADO", idFunc + " (" + nomeFunc + ")", "BLOQUEADO");
            salvarTodosArquivosTxtEBackup();
            return false;
        }

        System.out.print("Senha: ");
        String senhaDigitada = sc.nextLine().trim();

        if (!senhaCorreta.equals(senhaDigitada)) {
            tentativasErro++;
            f[11] = String.valueOf(tentativasErro);
            if (tentativasErro >= 3) {
                f[8] = "true";
                funcionarios.set(idx, montarLinha(f));
                System.out.println("[ALERTA] 3 tentativas incorretas! Conta " + idFunc + " bloqueada.");
                registrarAcesso("BLOQUEIO_3_TENTATIVAS", idFunc + " bloqueado apos 3 erros", "BLOQUEADO");
            } else {
                funcionarios.set(idx, montarLinha(f));
                System.out.println("[AVISO] Senha incorreta. Tentativa " + tentativasErro + " de 3.");
                registrarAcesso("SENHA_INCORRETA", idFunc + " tentativa " + tentativasErro + "/3", "FALHA");
            }
            salvarTodosArquivosTxtEBackup();
            return false;
        }

        if ("GERENCIA".equalsIgnoreCase(cargoFunc) || "GERENCIA".equalsIgnoreCase(autoridade)) {
            System.out.print("Confirmacao extra de GERENCIA - digite a senha novamente: ");
            String confSenha = sc.nextLine().trim();
            if (!senhaCorreta.equals(confSenha)) {
                System.out.println("[AVISO] Confirmacao de senha da gerencia nao confere.");
                registrarAcesso("LOGIN_GERENCIA_FALHA", idFunc, "NEGADO");
                salvarTodosArquivosTxtEBackup();
                return false;
            }
        }

        f[11] = "0";
        funcionarios.set(idx, montarLinha(f));
        usuarioLogadoId = idFunc;
        usuarioLogadoNome = nomeFunc;
        usuarioLogadoCargo = autoridade.toUpperCase(Locale.ROOT);

        registrarAcesso("LOGIN_SUCESSO", idFunc + " | Cargo/Autoridade: " + usuarioLogadoCargo, "OK");
        salvarTodosArquivosTxtEBackup();
        System.out.println("[OK] Bem-vindo(a), " + usuarioLogadoNome + " (" + usuarioLogadoCargo + ").");
        return true;
    }

    private static boolean possuiAutoridade(String... cargosPermitidos) {
        if (usuarioLogadoId == null || usuarioLogadoCargo == null) return false;
        int idx = encontrarIndicePorChave(funcionarios, 0, usuarioLogadoId);
        if (idx == -1) return false;
        String[] f = partirLinha(funcionarios.get(idx));
        boolean ativo = Boolean.parseBoolean(f[9]);
        boolean bloqueado = Boolean.parseBoolean(f[8]);
        if (!ativo || bloqueado) {
            return false;
        }
        String autAtual = f.length > 13 ? f[13].trim().toUpperCase(Locale.ROOT) : usuarioLogadoCargo;
        usuarioLogadoCargo = autAtual;
        if ("GERENCIA".equals(autAtual)) return true;
        for (String permitido : cargosPermitidos) {
            if (autAtual.equalsIgnoreCase(permitido)) return true;
        }
        return false;
    }

    private static void menuFuncionarioAutenticado(Scanner sc) {
        while (usuarioLogadoId != null) {
            if (!possuiAutoridade("GERENCIA", "RH", "ESTOQUE", "CAIXA", "FINANCEIRO", "TI", "FUNCIONARIO")) {
                System.out.println("[ALERTA] Sua conta foi desativada ou bloqueada durante a sessao.");
                voltarAoInicioDeslogando("CONTA_INATIVA_OU_BLOQUEADA");
                break;
            }

            System.out.println("\n=== PAINEL INTERNO | Usuario: " + usuarioLogadoNome + " (" + usuarioLogadoId
                    + ") | Autoridade: " + usuarioLogadoCargo + " | Caixa: " + formatarMoeda(saldoCaixa) + " ===");
            System.out.println("1 - Meu Perfil (dados e idade calculada)");

            boolean verEstoque = possuiAutoridade("ESTOQUE", "CAIXA", "FINANCEIRO", "GERENCIA");
            boolean verRh = possuiAutoridade("RH", "GERENCIA");
            boolean verCaixa = possuiAutoridade("CAIXA", "GERENCIA");
            boolean verFinanceiro = possuiAutoridade("FINANCEIRO", "GERENCIA");
            boolean verTi = possuiAutoridade("TI", "GERENCIA");
            boolean verGerencia = possuiAutoridade("GERENCIA");

            if (verEstoque) System.out.println("2 - Estoque e Fornecedores (produtos, compras, movimentacoes, exclusao)");
            if (verRh) System.out.println("3 - RH (contratar, editar modal, demitir com motivo, readmitir)");
            if (verCaixa) System.out.println("4 - Caixa (registrar venda e emitir nota fiscal)");
            if (verFinanceiro) System.out.println("5 - Financeiro (salarios, fornecedores, aliquotas ano atual/prox, lucro)");
            if (verTi) System.out.println("6 - TI (desbloquear login, autoridades, historico acessos, ver .TXT, backup)");
            if (verGerencia) System.out.println("7 - Gerencia (visao consolidada do sistema e auditoria)");

            System.out.println("9 - Voltar para a tela inicial (desloga usuario)");
            System.out.println("0 - Encerrar sistema (backup automatico)");
            System.out.print("Escolha: ");

            String op = sc.nextLine().trim();
            switch (op) {
                case "1":
                    exibirMeuPerfil();
                    break;
                case "2":
                    if (verEstoque) menuEstoqueEFornecedores(sc);
                    else System.out.println("[NEGADO] Sem autoridade para Estoque e Fornecedores.");
                    break;
                case "3":
                    if (verRh) menuRh(sc);
                    else System.out.println("[NEGADO] Sem autoridade para RH.");
                    break;
                case "4":
                    if (verCaixa) registrarVendaSimples(sc);
                    else System.out.println("[NEGADO] Sem autoridade para Caixa.");
                    break;
                case "5":
                    if (verFinanceiro) menuFinanceiro(sc);
                    else System.out.println("[NEGADO] Sem autoridade para Financeiro.");
                    break;
                case "6":
                    if (verTi) menuTi(sc);
                    else System.out.println("[NEGADO] Sem autoridade para TI.");
                    break;
                case "7":
                    if (verGerencia) menuGerencia(sc);
                    else System.out.println("[NEGADO] Sem autoridade de Gerencia.");
                    break;
                case "9":
                    voltarAoInicioDeslogando("VOLTAR_AO_INICIO");
                    break;
                case "0":
                    salvarTodosArquivosTxtEBackup();
                    registrarAcesso("LOGOUT_ENCERRAR", "Encerramento pelo usuario " + usuarioLogadoId, "OK");
                    salvarTodosArquivosTxtEBackup();
                    System.exit(0);
                    break;
                default:
                    System.out.println("[AVISO] Opcao invalida.");
                    break;
            }
        }
    }

    private static void exibirMeuPerfil() {
        int idx = encontrarIndicePorChave(funcionarios, 0, usuarioLogadoId);
        if (idx == -1) {
            System.out.println("[AVISO] Perfil nao encontrado.");
            return;
        }
        String[] f = partirLinha(funcionarios.get(idx));
        int idade = calcularIdade(f[3]);
        System.out.println("\n--- MEU PERFIL ---");
        System.out.println("ID: " + f[0]);
        System.out.println("Nome: " + f[1]);
        System.out.println("CPF: " + f[2]);
        System.out.println("Data de Nascimento: " + f[3] + (idade >= 0 ? " (Idade: " + idade + " anos)" : " (Data invalida)"));
        System.out.println("Sexo: " + f[4]);
        System.out.println("Cargo: " + f[5]);
        System.out.println("Salario: " + formatarMoeda(Double.parseDouble(f[7])));
        System.out.println("Ativo: " + f[9] + " | Bloqueado: " + f[8] + " | Pago: " + f[10]);
        System.out.println("Autoridade: " + (f.length > 13 ? f[13] : f[5]));
        registrarAcesso("CONSULTA_MEU_PERFIL", f[0], "OK");
        salvarTodosArquivosTxtEBackup();
    }

    // =========================================================================
    // 5. PORTAL DO CLIENTE: VITRINE (ATE 25 PAGINAS), FILTRO, CARRINHO E NF
    // =========================================================================

    private static void menuCliente(Scanner sc) {
        String filtroAtual = "";
        while (true) {
            System.out.println("\n=== PORTAL DO CLIENTE ===");
            System.out.println("Filtro ativo: " + (filtroAtual.isEmpty() ? "[NENHUM]" : filtroAtual)
                    + " | Itens no carrinho: " + carrinhoCliente.size());
            System.out.println("1 - Listar produtos (ate 25 paginas, navegacao segura)");
            System.out.println("2 - Criar / alterar filtro de pesquisa de produtos");
            System.out.println("3 - Limpar filtro de pesquisa");
            System.out.println("4 - Adicionar produto ao carrinho");
            System.out.println("5 - Ver carrinho / alterar quantidade / remover item");
            System.out.println("6 - Finalizar compra e emitir Nota Fiscal (exige CPF e Nome)");
            System.out.println("0 - Voltar ao inicio");
            System.out.print("Escolha: ");

            String op = sc.nextLine().trim();
            switch (op) {
                case "1":
                    listarProdutosClientePaginado(sc, filtroAtual);
                    break;
                case "2":
                    System.out.print("Digite termo de busca (nome ou codigo): ");
                    filtroAtual = sc.nextLine().trim();
                    System.out.println("[OK] Filtro aplicado: " + filtroAtual);
                    break;
                case "3":
                    filtroAtual = "";
                    System.out.println("[OK] Filtro removido.");
                    break;
                case "4":
                    adicionarProdutoAoCarrinho(sc);
                    break;
                case "5":
                    gerenciarCarrinho(sc);
                    break;
                case "6":
                    finalizarCompraCliente(sc);
                    break;
                case "0":
                    return;
                default:
                    System.out.println("[AVISO] Opcao invalida.");
                    break;
            }
        }
    }

    private static List<String> obterProdutosAtivosFiltrados(String filtro) {
        List<String> filtrados = new ArrayList<>();
        String termo = filtro == null ? "" : filtro.trim().toLowerCase(Locale.ROOT);
        for (String linha : produtos) {
            String[] p = partirLinha(linha);
            if (p.length < 9) continue;
            boolean ativo = Boolean.parseBoolean(p[8]);
            if (!ativo) continue;
            if (termo.isEmpty()
                    || p[0].toLowerCase(Locale.ROOT).contains(termo)
                    || p[1].toLowerCase(Locale.ROOT).contains(termo)) {
                filtrados.add(linha);
            }
        }
        return filtrados;
    }

    private static void listarProdutosClientePaginado(Scanner sc, String filtro) {
        List<String> lista = obterProdutosAtivosFiltrados(filtro);
        if (lista.isEmpty()) {
            System.out.println("[AVISO] Nenhum produto encontrado para o filtro informado.");
            return;
        }
        List<String> formatados = new ArrayList<>();
        for (String linha : lista) {
            String[] p = partirLinha(linha);
            double preco = Double.parseDouble(p[4]);
            formatados.add(montarLinha(p[0], p[1], "Preco: " + formatarMoeda(preco),
                    "Estoque: " + p[5], "Aliq: " + p[6] + "%", "Fornecedor: " + p[2]));
        }
        exibirListaPaginada(sc, "VITRINE DE PRODUTOS", formatados, 10, 25);
    }

    private static void adicionarProdutoAoCarrinho(Scanner sc) {
        String cod = normalizarTextoObrigatorio(sc, "Codigo do produto (ex.: PRD-001)", true);
        if (cod == null) return;

        int idx = encontrarIndicePorChave(produtos, 0, cod);
        if (idx == -1) {
            System.out.println("[AVISO] Produto '" + cod + "' nao existe.");
            return;
        }

        String[] p = partirLinha(produtos.get(idx));
        if (!Boolean.parseBoolean(p[8])) {
            System.out.println("[AVISO] Produto inativo/excluido do catalogo.");
            return;
        }

        int estoqueDisp = Integer.parseInt(p[5]);
        Integer qtd = lerInteiroSeguro(sc, "Quantidade (Disponivel: " + estoqueDisp + ")", 1, null, true);
        if (qtd == null) return;

        int qtdJaNoCarrinho = obterQuantidadeNoCarrinho(p[0]);
        if (qtdJaNoCarrinho + qtd > estoqueDisp) {
            System.out.println("[AVISO] Quantidade solicitada excede o estoque disponivel ("
                    + estoqueDisp + " no estoque, " + qtdJaNoCarrinho + " ja no carrinho).");
            return;
        }

        double custoUnit = Double.parseDouble(p[3]);
        double precoUnit = Double.parseDouble(p[4]);
        double aliqAtual = Double.parseDouble(p[6]);

        int idxCar = encontrarIndicePorChave(carrinhoCliente, 0, p[0]);
        if (idxCar >= 0) {
            String[] c = partirLinha(carrinhoCliente.get(idxCar));
            int novaQtd = Integer.parseInt(c[3]) + qtd;
            carrinhoCliente.set(idxCar, montarLinha(p[0], p[1], p[2], String.valueOf(novaQtd),
                    String.format(Locale.US, "%.2f", precoUnit),
                    String.format(Locale.US, "%.2f", custoUnit),
                    String.format(Locale.US, "%.2f", aliqAtual)));
        } else {
            carrinhoCliente.add(montarLinha(p[0], p[1], p[2], String.valueOf(qtd),
                    String.format(Locale.US, "%.2f", precoUnit),
                    String.format(Locale.US, "%.2f", custoUnit),
                    String.format(Locale.US, "%.2f", aliqAtual)));
        }
        System.out.println("[OK] Adicionado ao carrinho: " + p[1] + " x" + qtd);
    }

    private static int obterQuantidadeNoCarrinho(String codPrd) {
        int idx = encontrarIndicePorChave(carrinhoCliente, 0, codPrd);
        if (idx == -1) return 0;
        String[] c = partirLinha(carrinhoCliente.get(idx));
        return Integer.parseInt(c[3]);
    }

    private static void gerenciarCarrinho(Scanner sc) {
        while (true) {
            System.out.println("\n--- CARRINHO DE COMPRAS ---");
            if (carrinhoCliente.isEmpty()) {
                System.out.println("Carrinho vazio.");
                return;
            }
            double totalBruto = 0.0;
            for (int i = 0; i < carrinhoCliente.size(); i++) {
                String[] c = partirLinha(carrinhoCliente.get(i));
                int qtd = Integer.parseInt(c[3]);
                double unit = Double.parseDouble(c[4]);
                double sub = Math.round(qtd * unit * 100.0) / 100.0;
                totalBruto += sub;
                System.out.println((i + 1) + ". [" + c[0] + "] " + c[1] + " | Qtd: " + qtd
                        + " | Unit: " + formatarMoeda(unit) + " | Subtotal: " + formatarMoeda(sub));
            }
            System.out.println("Total parcial (sem imposto): " + formatarMoeda(totalBruto));
            System.out.println("1 - Alterar quantidade de um item");
            System.out.println("2 - Remover item do carrinho");
            System.out.println("0 - Voltar");
            System.out.print("Escolha: ");
            String op = sc.nextLine().trim();

            if ("1".equals(op)) {
                String cod = normalizarTextoObrigatorio(sc, "Codigo do produto no carrinho", true);
                if (cod == null) continue;
                int idxCar = encontrarIndicePorChave(carrinhoCliente, 0, cod);
                if (idxCar == -1) {
                    System.out.println("[AVISO] Item nao encontrado no carrinho.");
                    continue;
                }
                int idxPrd = encontrarIndicePorChave(produtos, 0, cod);
                int estDisp = idxPrd >= 0 ? Integer.parseInt(partirLinha(produtos.get(idxPrd))[5]) : 0;
                Integer novaQtd = lerInteiroSeguro(sc, "Nova quantidade (Estoque: " + estDisp + ")", 0, null, true);
                if (novaQtd == null) continue;
                if (novaQtd == 0) {
                    carrinhoCliente.remove(idxCar);
                    System.out.println("[OK] Item removido.");
                } else if (novaQtd > estDisp) {
                    System.out.println("[AVISO] Quantidade excede o estoque disponivel (" + estDisp + ").");
                } else {
                    String[] c = partirLinha(carrinhoCliente.get(idxCar));
                    c[3] = String.valueOf(novaQtd);
                    carrinhoCliente.set(idxCar, montarLinha(c));
                    System.out.println("[OK] Quantidade atualizada.");
                }
            } else if ("2".equals(op)) {
                String cod = normalizarTextoObrigatorio(sc, "Codigo do produto para remover", true);
                if (cod == null) continue;
                int idxCar = encontrarIndicePorChave(carrinhoCliente, 0, cod);
                if (idxCar >= 0) {
                    carrinhoCliente.remove(idxCar);
                    System.out.println("[OK] Item removido do carrinho.");
                } else {
                    System.out.println("[AVISO] Item nao encontrado no carrinho.");
                }
            } else if ("0".equals(op)) {
                return;
            }
        }
    }

    private static void finalizarCompraCliente(Scanner sc) {
        if (carrinhoCliente.isEmpty()) {
            System.out.println("[AVISO] Carrinho vazio. Adicione produtos antes de finalizar.");
            return;
        }

        System.out.println("\n--- FINALIZACAO DE COMPRA (CPF e Nome obrigatorios) ---");
        String cpfInput = normalizarTextoObrigatorio(sc, "CPF do cliente", true);
        if (cpfInput == null) {
            System.out.println("[INFO] Compra cancelada pelo cliente.");
            return;
        }
        String cpfLimpo = cpfInput.replaceAll("\\D", "");
        if (!isCpfValido(cpfLimpo)) {
            System.out.println("[AVISO] CPF invalido. Deve conter 11 digitos.");
            return;
        }

        int idxCli = encontrarIndicePorChave(clientes, 1, cpfLimpo);
        String nomeCliente;
        if (idxCli >= 0) {
            String[] cliExistente = partirLinha(clientes.get(idxCli));
            nomeCliente = cliExistente[2];
            System.out.println("[INFO] Cliente ja cadastrado: " + nomeCliente + " (" + cliExistente[0] + ")");
            System.out.print("Confirmar nome ou digitar novo nome (Enter para manter): ");
            String novoNome = sc.nextLine().trim();
            if (!novoNome.isEmpty()) {
                nomeCliente = novoNome;
                cliExistente[2] = nomeCliente;
                clientes.set(idxCli, montarLinha(cliExistente));
            }
        } else {
            nomeCliente = normalizarTextoObrigatorio(sc, "Nome completo do cliente (novo cadastro)", true);
            if (nomeCliente == null) {
                System.out.println("[INFO] Compra cancelada pelo cliente.");
                return;
            }
            String idCli = String.format("CLI-%03d", proxCli++);
            clientes.add(montarLinha(idCli, cpfLimpo, nomeCliente));
            System.out.println("[OK] Novo cliente registrado: " + idCli + " - " + nomeCliente);
        }

        // Conferencia final de estoque antes de efetivar
        for (String itemLinha : carrinhoCliente) {
            String[] c = partirLinha(itemLinha);
            int qtdDesejada = Integer.parseInt(c[3]);
            int idxPrd = encontrarIndicePorChave(produtos, 0, c[0]);
            if (idxPrd == -1) {
                System.out.println("[AVISO] Produto " + c[0] + " nao existe mais.");
                return;
            }
            String[] p = partirLinha(produtos.get(idxPrd));
            int estAtual = Integer.parseInt(p[5]);
            if (qtdDesejada > estAtual) {
                System.out.println("[AVISO] Estoque insuficiente para " + p[1] + " (Disponivel: " + estAtual + ").");
                return;
            }
        }

        double subtotalGeral = 0.0;
        double impostoGeral = 0.0;
        double custoGeral = 0.0;
        List<String> resumoItens = new ArrayList<>();

        for (String itemLinha : carrinhoCliente) {
            String[] c = partirLinha(itemLinha);
            String codPrd = c[0];
            String nomePrd = c[1];
            String forId = c[2];
            int qtd = Integer.parseInt(c[3]);
            double precoUnit = Double.parseDouble(c[4]);
            double custoUnit = Double.parseDouble(c[5]);
            double aliq = Double.parseDouble(c[6]);

            int idxPrd = encontrarIndicePorChave(produtos, 0, codPrd);
            String[] p = partirLinha(produtos.get(idxPrd));
            int novoEst = Integer.parseInt(p[5]) - qtd;
            p[5] = String.valueOf(novoEst);
            produtos.set(idxPrd, montarLinha(p));

            double sub = Math.round(precoUnit * qtd * 100.0) / 100.0;
            double imp = Math.round(sub * (aliq / 100.0) * 100.0) / 100.0;
            double cst = Math.round(custoUnit * qtd * 100.0) / 100.0;

            subtotalGeral = Math.round((subtotalGeral + sub) * 100.0) / 100.0;
            impostoGeral = Math.round((impostoGeral + imp) * 100.0) / 100.0;
            custoGeral = Math.round((custoGeral + cst) * 100.0) / 100.0;
            resumoItens.add(codPrd + "x" + qtd);

            registrarMovEstoque("SAIDA", codPrd, nomePrd, forId, qtd, "Venda portal cliente");
        }

        double lucroLiquido = Math.round((subtotalGeral - custoGeral - impostoGeral) * 100.0) / 100.0;
        double totalNf = Math.round((subtotalGeral + impostoGeral) * 100.0) / 100.0;
        String nfStr = "NF-" + (proxNf++);
        String itensConcat = String.join(",", resumoItens);

        histVendas.add(montarLinha(
                nfStr, dataHojeFormatada(), cpfLimpo, nomeCliente, itensConcat,
                String.format(Locale.US, "%.2f", subtotalGeral),
                String.format(Locale.US, "%.2f", impostoGeral),
                String.format(Locale.US, "%.2f", custoGeral),
                String.format(Locale.US, "%.2f", lucroLiquido),
                String.format(Locale.US, "%.2f", totalNf)
        ));

        registrarLancamentoFinanceiro("VENDA_NF", "Nota Fiscal " + nfStr + " - " + nomeCliente, totalNf);
        registrarAcesso("COMPRA_FINALIZADA", nfStr + " Cliente: " + nomeCliente + " Total: " + totalNf, "OK");
        carrinhoCliente.clear();
        salvarTodosArquivosTxtEBackup();

        System.out.println("\n================ NOTA FISCAL EMITIDA ================");
        System.out.println("Numero NF: " + nfStr + " | Data: " + dataHojeFormatada());
        System.out.println("Cliente: " + nomeCliente + " | CPF: " + cpfLimpo);
        System.out.println("Itens: " + itensConcat);
        System.out.println("Subtotal: " + formatarMoeda(subtotalGeral));
        System.out.println("Impostos: " + formatarMoeda(impostoGeral));
        System.out.println("Total a Pagar: " + formatarMoeda(totalNf));
        System.out.println("=====================================================");
    }

    // =========================================================================
    // 6. ESTOQUE E FORNECEDORES
    // =========================================================================

    private static void menuEstoqueEFornecedores(Scanner sc) {
        while (true) {
            System.out.println("\n=== ESTOQUE E FORNECEDORES ===");
            System.out.println("1 - Listar produtos (com alerta de estoque baixo/zerado)");
            System.out.println("2 - Cadastrar produto vinculado a fornecedor (exige precoVenda > custo)");
            System.out.println("3 - Cadastrar fornecedor (com ID e UF de fornecimento)");
            System.out.println("4 - Comprar produto de fornecedor (valida saldo, atualiza estoque e financeiro)");
            System.out.println("5 - Registrar entrada/saida manual de estoque");
            System.out.println("6 - Editar produto via modal");
            System.out.println("7 - Excluir produto (mantem registro no historico de estoque)");
            System.out.println("8 - Ver historico de movimentacoes de estoque");
            System.out.println("0 - Voltar ao painel");
            System.out.print("Escolha: ");

            String op = sc.nextLine().trim();
            switch (op) {
                case "1":
                    listarProdutosInterno(sc);
                    break;
                case "2":
                    cadastrarProdutoVinculado(sc);
                    break;
                case "3":
                    cadastrarFornecedor(sc);
                    break;
                case "4":
                    comprarDeFornecedor(sc);
                    break;
                case "5":
                    movimentacaoManualEstoque(sc);
                    break;
                case "6":
                    editarProdutoModal(sc);
                    break;
                case "7":
                    excluirProdutoMantendoHistorico(sc);
                    break;
                case "8":
                    exibirListaPaginada(sc, "HISTORICO DE ESTOQUE", histEstoque, 10, 0);
                    break;
                case "0":
                    return;
                default:
                    System.out.println("[AVISO] Opcao invalida.");
                    break;
            }
        }
    }

    private static void listarProdutosInterno(Scanner sc) {
        List<String> linhas = new ArrayList<>();
        for (String raw : produtos) {
            String[] p = partirLinha(raw);
            int est = Integer.parseInt(p[5]);
            String alerta = est == 0 ? "[SEM ESTOQUE]" : (est <= 5 ? "[ESTOQUE BAIXO]" : "[OK]");
            String status = Boolean.parseBoolean(p[8]) ? "ATIVO" : "EXCLUIDO";
            linhas.add(montarLinha(p[0], p[1], "For:" + p[2],
                    "Custo:" + formatarMoeda(Double.parseDouble(p[3])),
                    "Venda:" + formatarMoeda(Double.parseDouble(p[4])),
                    "Est:" + est + " " + alerta,
                    "Aliq:" + p[6] + "%/" + p[7] + "%", status));
        }
        exibirListaPaginada(sc, "CATALOGO DE PRODUTOS", linhas, 10, 0);
    }

    private static void cadastrarFornecedor(Scanner sc) {
        if (produtos.isEmpty()) {
            System.out.println("[AVISO] Todo fornecedor deve fornecer/vincular um produto. Cadastre um produto primeiro.");
            return;
        }
        String idFor = normalizarTextoObrigatorio(sc, "ID do fornecedor (ex.: FOR-002)", true);
        if (idFor == null) return;
        if (encontrarIndicePorChave(fornecedores, 0, idFor) >= 0) {
            System.out.println("[AVISO] ID de fornecedor ja existe.");
            return;
        }
        String nome = normalizarTextoObrigatorio(sc, "Razao social / Nome do fornecedor", true);
        if (nome == null) return;
        String cnpj = normalizarTextoObrigatorio(sc, "CNPJ (14 digitos)", true);
        if (cnpj == null) return;
        String cnpjLimpo = cnpj.replaceAll("\\D", "");
        if (!isCnpjValido(cnpjLimpo)) {
            System.out.println("[AVISO] CNPJ invalido.");
            return;
        }
        String uf = normalizarTextoObrigatorio(sc, "Estado/UF de fornecimento (ex.: SP, RJ, MG)", true);
        if (uf == null) return;
        uf = uf.toUpperCase(Locale.ROOT);
        if (!isUfValida(uf)) {
            System.out.println("[AVISO] UF invalida.");
            return;
        }
        String codPrd = normalizarTextoObrigatorio(sc, "Codigo do produto fornecido (ex.: PRD-001)", true);
        if (codPrd == null) return;
        if (encontrarIndicePorChave(produtos, 0, codPrd) == -1) {
            System.out.println("[AVISO] Produto " + codPrd + " nao encontrado.");
            return;
        }
        Double custo = lerDecimalSeguro(sc, "Custo unitario praticado pelo fornecedor", 0.01, true);
        if (custo == null) return;

        fornecedores.add(montarLinha(idFor.toUpperCase(Locale.ROOT), nome, cnpjLimpo, uf,
                codPrd.toUpperCase(Locale.ROOT), String.format(Locale.US, "%.2f", custo)));
        registrarAcesso("CADASTRO_FORNECEDOR", idFor + " (" + uf + ")", "OK");
        salvarTodosArquivosTxtEBackup();
        System.out.println("[OK] Fornecedor cadastrado com sucesso.");
    }

    private static void cadastrarProdutoVinculado(Scanner sc) {
        if (fornecedores.isEmpty()) {
            System.out.println("[AVISO] Todo produto deve ter um fornecedor. Cadastre um fornecedor primeiro.");
            return;
        }
        String codPrd = normalizarTextoObrigatorio(sc, "Codigo do produto (ex.: PRD-002)", true);
        if (codPrd == null) return;
        if (encontrarIndicePorChave(produtos, 0, codPrd) >= 0) {
            System.out.println("[AVISO] Codigo de produto ja cadastrado.");
            return;
        }
        String nomePrd = normalizarTextoObrigatorio(sc, "Nome do produto", true);
        if (nomePrd == null) return;
        String forId = normalizarTextoObrigatorio(sc, "ID do fornecedor responsavel", true);
        if (forId == null) return;
        if (encontrarIndicePorChave(fornecedores, 0, forId) == -1) {
            System.out.println("[AVISO] Fornecedor '" + forId + "' nao existe.");
            return;
        }
        Double custo = lerDecimalSeguro(sc, "Custo unitario do fornecedor", 0.01, true);
        if (custo == null) return;
        Double precoVenda = lerDecimalSeguro(sc, "Preco de venda ao cliente", 0.01, true);
        if (precoVenda == null) return;
        if (precoVenda <= custo) {
            System.out.println("[AVISO] O preco de venda (" + formatarMoeda(precoVenda)
                    + ") deve ser maior que o custo do fornecedor (" + formatarMoeda(custo) + ").");
            return;
        }
        Integer estoqueIni = lerInteiroSeguro(sc, "Quantidade inicial em estoque", 0, null, true);
        if (estoqueIni == null) return;

        String codUpper = codPrd.toUpperCase(Locale.ROOT);
        produtos.add(montarLinha(codUpper, nomePrd, forId.toUpperCase(Locale.ROOT),
                String.format(Locale.US, "%.2f", custo),
                String.format(Locale.US, "%.2f", precoVenda),
                String.valueOf(estoqueIni),
                String.format(Locale.US, "%.2f", aliquotaAnoAtual),
                String.format(Locale.US, "%.2f", aliquotaProximoAno),
                "true"));

        if (estoqueIni > 0) {
            registrarMovEstoque("ENTRADA", codUpper, nomePrd, forId.toUpperCase(Locale.ROOT),
                    estoqueIni, "Cadastro inicial de produto");
        }
        registrarAcesso("CADASTRO_PRODUTO", codUpper + " (" + nomePrd + ")", "OK");
        salvarTodosArquivosTxtEBackup();
        System.out.println("[OK] Produto cadastrado e sincronizado.");
    }

    private static void comprarDeFornecedor(Scanner sc) {
        String codPrd = normalizarTextoObrigatorio(sc, "Codigo do produto para compra", true);
        if (codPrd == null) return;
        int idxPrd = encontrarIndicePorChave(produtos, 0, codPrd);
        if (idxPrd == -1) {
            System.out.println("[AVISO] Produto nao encontrado.");
            return;
        }
        String[] p = partirLinha(produtos.get(idxPrd));
        String forId = p[2];
        double custoUnit = Double.parseDouble(p[3]);
        double precoVenda = Double.parseDouble(p[4]);
        if (precoVenda <= custoUnit) {
            System.out.println("[AVISO] Preco de venda atual nao e maior que o custo (" + formatarMoeda(custoUnit) + ").");
            return;
        }

        Integer qtd = lerInteiroSeguro(sc, "Quantidade a comprar do fornecedor " + forId
                + " (Custo unit: " + formatarMoeda(custoUnit) + ")", 1, null, true);
        if (qtd == null) return;

        double valorCompra = Math.round(custoUnit * qtd * 100.0) / 100.0;
        if (saldoCaixa < valorCompra) {
            System.out.println("[AVISO] Saldo insuficiente em caixa (" + formatarMoeda(saldoCaixa)
                    + ") para compra de " + formatarMoeda(valorCompra) + ".");
            return;
        }

        int novoEst = Integer.parseInt(p[5]) + qtd;
        p[5] = String.valueOf(novoEst);
        p[8] = "true";
        produtos.set(idxPrd, montarLinha(p));

        registrarLancamentoFinanceiro("COMPRA_FORNECEDOR", "Compra " + p[0] + " x" + qtd + " (" + forId + ")", -valorCompra);
        registrarMovEstoque("ENTRADA", p[0], p[1], forId, qtd, "Compra de fornecedor " + forId);
        registrarAcesso("COMPRA_FORNECEDOR", p[0] + " x" + qtd + " por " + formatarMoeda(valorCompra), "OK");
        salvarTodosArquivosTxtEBackup();
        System.out.println("[OK] Compra concluida! Novo estoque: " + novoEst + " | Saldo: " + formatarMoeda(saldoCaixa));
    }

    private static void movimentacaoManualEstoque(Scanner sc) {
        String codPrd = normalizarTextoObrigatorio(sc, "Codigo do produto", true);
        if (codPrd == null) return;
        int idxPrd = encontrarIndicePorChave(produtos, 0, codPrd);
        if (idxPrd == -1) {
            System.out.println("[AVISO] Produto nao encontrado.");
            return;
        }
        String[] p = partirLinha(produtos.get(idxPrd));
        String tipo = normalizarTextoObrigatorio(sc, "Tipo (ENTRADA ou SAIDA)", true);
        if (tipo == null) return;
        tipo = tipo.toUpperCase(Locale.ROOT);
        if (!"ENTRADA".equals(tipo) && !"SAIDA".equals(tipo)) {
            System.out.println("[AVISO] Tipo deve ser ENTRADA ou SAIDA.");
            return;
        }
        Integer qtd = lerInteiroSeguro(sc, "Quantidade", 1, null, true);
        if (qtd == null) return;

        int estAtual = Integer.parseInt(p[5]);
        if ("SAIDA".equals(tipo) && estAtual < qtd) {
            System.out.println("[AVISO] Estoque insuficiente para saida. Disponivel: " + estAtual);
            return;
        }

        int novoEst = "ENTRADA".equals(tipo) ? estAtual + qtd : estAtual - qtd;
        p[5] = String.valueOf(novoEst);
        produtos.set(idxPrd, montarLinha(p));

        registrarMovEstoque(tipo, p[0], p[1], p[2], qtd, "Movimentacao manual");
        registrarAcesso("MOV_ESTOQUE_MANUAL", tipo + " " + p[0] + " x" + qtd, "OK");
        salvarTodosArquivosTxtEBackup();
        System.out.println("[OK] Movimentacao registrada. Novo estoque: " + novoEst);
    }

    private static void editarProdutoModal(Scanner sc) {
        String codPrd = normalizarTextoObrigatorio(sc, "Codigo do produto para editar", true);
        if (codPrd == null) return;
        int idx = encontrarIndicePorChave(produtos, 0, codPrd);
        if (idx == -1) {
            System.out.println("[AVISO] Produto nao encontrado.");
            return;
        }
        String[] p = partirLinha(produtos.get(idx));
        System.out.println("\n--- MODAL DE EDICAO DE PRODUTO [" + p[0] + "] (Enter mantem valor atual) ---");

        System.out.print("Nome (" + p[1] + "): ");
        String nome = sc.nextLine().trim();
        if (!nome.isEmpty()) p[1] = nome;

        System.out.print("Custo unitario (" + p[3] + "): ");
        String custoStr = sc.nextLine().trim().replace(",", ".");
        double custo = custoStr.isEmpty() ? Double.parseDouble(p[3]) : Double.parseDouble(custoStr);

        System.out.print("Preco de venda (" + p[4] + "): ");
        String precoStr = sc.nextLine().trim().replace(",", ".");
        double preco = precoStr.isEmpty() ? Double.parseDouble(p[4]) : Double.parseDouble(precoStr);

        if (preco <= custo) {
            System.out.println("[AVISO] Preco de venda deve ser maior que o custo. Edicao cancelada.");
            return;
        }

        p[3] = String.format(Locale.US, "%.2f", custo);
        p[4] = String.format(Locale.US, "%.2f", preco);
        produtos.set(idx, montarLinha(p));

        registrarAcesso("EDICAO_PRODUTO_MODAL", p[0], "OK");
        salvarTodosArquivosTxtEBackup();
        System.out.println("[OK] Produto atualizado via modal.");
    }

    private static void excluirProdutoMantendoHistorico(Scanner sc) {
        String codPrd = normalizarTextoObrigatorio(sc, "Codigo do produto para excluir do catalogo", true);
        if (codPrd == null) return;
        int idx = encontrarIndicePorChave(produtos, 0, codPrd);
        if (idx == -1) {
            System.out.println("[AVISO] Produto nao encontrado.");
            return;
        }
        String[] p = partirLinha(produtos.get(idx));
        int estAntes = Integer.parseInt(p[5]);
        p[5] = "0";
        p[8] = "false";
        produtos.set(idx, montarLinha(p));

        registrarMovEstoque("EXCLUSAO_CATALOGO", p[0], p[1], p[2], estAntes,
                "Produto removido do catalogo ativo (preservado no historico)");
        registrarAcesso("EXCLUSAO_PRODUTO", p[0], "OK");
        salvarTodosArquivosTxtEBackup();
        System.out.println("[OK] Produto marcado como excluido e preservado no historico de estoque.");
    }

    // =========================================================================
    // 7. MODULO RH (ADMISSAO, EDICAO MODAL, DEMISSAO COM MOTIVO, READMISSAO)
    // =========================================================================

    private static void menuRh(Scanner sc) {
        while (true) {
            System.out.println("\n=== MODULO RH ===");
            System.out.println("1 - Listar funcionarios (com idade calculada)");
            System.out.println("2 - Contratar / cadastrar funcionario");
            System.out.println("3 - Editar funcionario via modal");
            System.out.println("4 - Demitir funcionario (motivo obrigatorio; sem auto-demissao)");
            System.out.println("5 - Readmitir funcionario");
            System.out.println("0 - Voltar ao painel");
            System.out.print("Escolha: ");

            String op = sc.nextLine().trim();
            switch (op) {
                case "1":
                    listarFuncionariosRh(sc);
                    break;
                case "2":
                    contratarFuncionario(sc);
                    break;
                case "3":
                    editarFuncionarioModal(sc);
                    break;
                case "4":
                    demitirFuncionario(sc);
                    break;
                case "5":
                    readmitirFuncionario(sc);
                    break;
                case "0":
                    return;
                default:
                    System.out.println("[AVISO] Opcao invalida.");
                    break;
            }
        }
    }

    private static void listarFuncionariosRh(Scanner sc) {
        List<String> linhas = new ArrayList<>();
        for (String raw : funcionarios) {
            String[] f = partirLinha(raw);
            int idade = calcularIdade(f[3]);
            linhas.add(montarLinha(f[0], f[1], "CPF:" + f[2],
                    "Nasc:" + f[3] + (idade >= 0 ? " (" + idade + "a)" : ""),
                    "Sexo:" + f[4], "Cargo:" + f[5],
                    "Sal:" + formatarMoeda(Double.parseDouble(f[7])),
                    "Ativo:" + f[9], "Pago:" + f[10], "Motivo:" + f[12]));
        }
        exibirListaPaginada(sc, "QUADRO DE FUNCIONARIOS", linhas, 10, 0);
    }

    private static void contratarFuncionario(Scanner sc) {
        String nome = normalizarTextoObrigatorio(sc, "Nome completo do funcionario", true);
        if (nome == null) return;
        String cpf = normalizarTextoObrigatorio(sc, "CPF (11 digitos)", true);
        if (cpf == null) return;
        String cpfLimpo = cpf.replaceAll("\\D", "");
        if (!isCpfValido(cpfLimpo) || encontrarIndicePorChave(funcionarios, 2, cpfLimpo) >= 0) {
            System.out.println("[AVISO] CPF invalido ou ja cadastrado.");
            return;
        }
        String nasc = normalizarTextoObrigatorio(sc, "Data de nascimento (dd/MM/yyyy)", true);
        if (nasc == null) return;
        int idade = calcularIdade(nasc);
        if (idade < 16) {
            System.out.println("[AVISO] Data de nascimento invalida ou idade inferior a 16 anos.");
            return;
        }
        String sexo = normalizarTextoObrigatorio(sc, "Sexo (M/F/O)", true);
        if (sexo == null) return;
        String cargo = normalizarTextoObrigatorio(sc, "Cargo (FUNCIONARIO, ESTOQUE, CAIXA, FINANCEIRO, RH, TI, GERENCIA)", true);
        if (cargo == null) return;
        String senha = normalizarTextoObrigatorio(sc, "Senha inicial", true);
        if (senha == null) return;
        Double salario = lerDecimalSeguro(sc, "Salario inicial", 0.01, true);
        if (salario == null) return;

        String idFunc = String.format("FUNC-%03d", proxFunc++);
        String cargoUpper = cargo.toUpperCase(Locale.ROOT);
        funcionarios.add(montarLinha(idFunc, nome, cpfLimpo, nasc, sexo.toUpperCase(Locale.ROOT),
                cargoUpper, senha, String.format(Locale.US, "%.2f", salario),
                "false", "true", "false", "0", "N/A", cargoUpper));

        registrarAcesso("ADMISSAO_FUNCIONARIO", idFunc + " (" + nome + " - " + cargoUpper + ")", "OK");
        salvarTodosArquivosTxtEBackup();
        System.out.println("[OK] Funcionario contratado: " + idFunc + " | Idade: " + idade + " anos.");
    }

    private static void editarFuncionarioModal(Scanner sc) {
        String idFunc = normalizarTextoObrigatorio(sc, "ID do funcionario para editar", true);
        if (idFunc == null) return;
        int idx = encontrarIndicePorChave(funcionarios, 0, idFunc);
        if (idx == -1) {
            System.out.println("[AVISO] Funcionario nao encontrado.");
            return;
        }
        String[] f = partirLinha(funcionarios.get(idx));
        System.out.println("\n--- MODAL DE EDICAO DE FUNCIONARIO [" + f[0] + "] (Enter mantem valor atual) ---");

        System.out.print("Nome (" + f[1] + "): ");
        String nome = sc.nextLine().trim();
        if (!nome.isEmpty()) f[1] = nome;

        System.out.print("Data de nascimento dd/MM/yyyy (" + f[3] + "): ");
        String nasc = sc.nextLine().trim();
        if (!nasc.isEmpty()) {
            if (calcularIdade(nasc) < 16) {
                System.out.println("[AVISO] Data de nascimento invalida ou menor de 16 anos.");
                return;
            }
            f[3] = nasc;
        }

        System.out.print("Sexo (" + f[4] + "): ");
        String sexo = sc.nextLine().trim();
        if (!sexo.isEmpty()) f[4] = sexo.toUpperCase(Locale.ROOT);

        System.out.print("Cargo (" + f[5] + "): ");
        String cargo = sc.nextLine().trim();
        if (!cargo.isEmpty()) {
            f[5] = cargo.toUpperCase(Locale.ROOT);
            f[13] = f[5];
        }

        funcionarios.set(idx, montarLinha(f));
        registrarAcesso("EDICAO_FUNCIONARIO_MODAL", f[0], "OK");
        salvarTodosArquivosTxtEBackup();
        System.out.println("[OK] Dados do funcionario atualizados via modal.");
    }

    private static void demitirFuncionario(Scanner sc) {
        String idFunc = normalizarTextoObrigatorio(sc, "ID do funcionario para demitir", true);
        if (idFunc == null) return;
        if (idFunc.equalsIgnoreCase(usuarioLogadoId)) {
            System.out.println("[BLOQUEADO] Trava contra auto-demissao: voce nao pode demitir a propria conta logada.");
            return;
        }
        int idx = encontrarIndicePorChave(funcionarios, 0, idFunc);
        if (idx == -1) {
            System.out.println("[AVISO] Funcionario nao encontrado.");
            return;
        }
        String motivo = normalizarTextoObrigatorio(sc, "Motivo da demissao (obrigatorio)", true);
        if (motivo == null) return;

        String[] f = partirLinha(funcionarios.get(idx));
        f[9] = "false";
        f[12] = motivo;
        funcionarios.set(idx, montarLinha(f));

        registrarAcesso("DEMISSAO_FUNCIONARIO", f[0] + " | Motivo: " + motivo, "OK");
        salvarTodosArquivosTxtEBackup();
        System.out.println("[OK] Funcionario " + f[0] + " demitido e autoridade revogada.");
    }

    private static void readmitirFuncionario(Scanner sc) {
        String idFunc = normalizarTextoObrigatorio(sc, "ID do funcionario para readmitir", true);
        if (idFunc == null) return;
        int idx = encontrarIndicePorChave(funcionarios, 0, idFunc);
        if (idx == -1) {
            System.out.println("[AVISO] Funcionario nao encontrado.");
            return;
        }
        String[] f = partirLinha(funcionarios.get(idx));
        f[9] = "true";
        f[8] = "false";
        f[11] = "0";
        f[12] = "READMITIDO";
        funcionarios.set(idx, montarLinha(f));

        registrarAcesso("READMISSAO_FUNCIONARIO", f[0], "OK");
        salvarTodosArquivosTxtEBackup();
        System.out.println("[OK] Funcionario " + f[0] + " readmitido com sucesso.");
    }

    // =========================================================================
    // 8. MODULO FINANCEIRO (SALARIOS, ALIQUOTAS, LUCRO E HISTORICO)
    // =========================================================================

    private static void menuFinanceiro(Scanner sc) {
        while (true) {
            System.out.println("\n=== MODULO FINANCEIRO (Caixa: " + formatarMoeda(saldoCaixa) + ") ===");
            System.out.println("Aliquotas configuradas -> Ano Atual: " + aliquotaAnoAtual
                    + "% | Proximo Ano: " + aliquotaProximoAno + "%");
            System.out.println("1 - Pagar salario de funcionario (valida pagamento previo e saldo)");
            System.out.println("2 - Ajustar salario de funcionario");
            System.out.println("3 - Definir aliquotas fiscais (ano atual e proximo ano)");
            System.out.println("4 - Demonstrativo de vendas, impostos e lucro liquido");
            System.out.println("5 - Ver historico financeiro completo");
            System.out.println("0 - Voltar ao painel");
            System.out.print("Escolha: ");

            String op = sc.nextLine().trim();
            switch (op) {
                case "1":
                    pagarSalarioFuncionario(sc);
                    break;
                case "2":
                    ajustarSalarioFuncionario(sc);
                    break;
                case "3":
                    definirAliquotasFiscais(sc);
                    break;
                case "4":
                    exibirDemonstrativoLucro();
                    break;
                case "5":
                    exibirListaPaginada(sc, "HISTORICO FINANCEIRO", histFinanceiro, 10, 0);
                    break;
                case "0":
                    return;
                default:
                    System.out.println("[AVISO] Opcao invalida.");
                    break;
            }
        }
    }

    private static void pagarSalarioFuncionario(Scanner sc) {
        String idFunc = normalizarTextoObrigatorio(sc, "ID do funcionario para pagamento de salario", true);
        if (idFunc == null) return;
        int idx = encontrarIndicePorChave(funcionarios, 0, idFunc);
        if (idx == -1) {
            System.out.println("[AVISO] Funcionario nao encontrado.");
            return;
        }
        String[] f = partirLinha(funcionarios.get(idx));
        if (!Boolean.parseBoolean(f[9])) {
            System.out.println("[AVISO] Funcionario inativo/demitido.");
            return;
        }
        if (Boolean.parseBoolean(f[10])) {
            System.out.println("[AVISO] Este funcionario ja teve o salario pago neste ciclo.");
            return;
        }
        double salario = Double.parseDouble(f[7]);
        if (saldoCaixa < salario) {
            System.out.println("[AVISO] Saldo insuficiente em caixa (" + formatarMoeda(saldoCaixa)
                    + ") para pagar salario de " + formatarMoeda(salario) + ".");
            return;
        }

        f[10] = "true";
        funcionarios.set(idx, montarLinha(f));
        registrarLancamentoFinanceiro("PAGAMENTO_SALARIO", "Salario " + f[0] + " - " + f[1], -salario);
        registrarAcesso("PAGAMENTO_SALARIO", f[0] + " (" + formatarMoeda(salario) + ")", "OK");
        salvarTodosArquivosTxtEBackup();
        System.out.println("[OK] Salario pago com sucesso! Novo saldo: " + formatarMoeda(saldoCaixa));
    }

    private static void ajustarSalarioFuncionario(Scanner sc) {
        String idFunc = normalizarTextoObrigatorio(sc, "ID do funcionario para ajustar salario", true);
        if (idFunc == null) return;
        int idx = encontrarIndicePorChave(funcionarios, 0, idFunc);
        if (idx == -1) {
            System.out.println("[AVISO] Funcionario nao encontrado.");
            return;
        }
        String[] f = partirLinha(funcionarios.get(idx));
        Double novoSal = lerDecimalSeguro(sc, "Novo salario (Atual: " + formatarMoeda(Double.parseDouble(f[7])) + ")",
                0.01, true);
        if (novoSal == null) return;

        f[7] = String.format(Locale.US, "%.2f", novoSal);
        f[10] = "false";
        funcionarios.set(idx, montarLinha(f));
        registrarAcesso("AJUSTE_SALARIO", f[0] + " -> " + formatarMoeda(novoSal), "OK");
        salvarTodosArquivosTxtEBackup();
        System.out.println("[OK] Salario ajustado para " + formatarMoeda(novoSal) + ".");
    }

    private static void definirAliquotasFiscais(Scanner sc) {
        Double atual = lerDecimalSeguro(sc, "Aliquota do ano atual (%) [Atual: " + aliquotaAnoAtual + "]", 0.0, true);
        if (atual == null) return;
        Double prox = lerDecimalSeguro(sc, "Aliquota do proximo ano (%) [Atual: " + aliquotaProximoAno + "]", 0.0, true);
        if (prox == null) return;

        aliquotaAnoAtual = atual;
        aliquotaProximoAno = prox;

        for (int i = 0; i < produtos.size(); i++) {
            String[] p = partirLinha(produtos.get(i));
            p[6] = String.format(Locale.US, "%.2f", aliquotaAnoAtual);
            p[7] = String.format(Locale.US, "%.2f", aliquotaProximoAno);
            produtos.set(i, montarLinha(p));
        }

        registrarAcesso("DEFINIR_ALIQUOTAS", "Atual=" + aliquotaAnoAtual + "% | Prox=" + aliquotaProximoAno + "%", "OK");
        salvarTodosArquivosTxtEBackup();
        System.out.println("[OK] Aliquotas atualizadas nos produtos e no backup.");
    }

    private static void exibirDemonstrativoLucro() {
        double totalBruto = 0.0;
        double totalImposto = 0.0;
        double totalLucro = 0.0;

        for (String v : histVendas) {
            String[] vp = partirLinha(v);
            totalBruto += Double.parseDouble(vp[5]);
            totalImposto += Double.parseDouble(vp[6]);
            totalLucro += Double.parseDouble(vp[8]);
        }

        double margemMedia = dividirSeguro(totalLucro * 100.0, totalBruto);
        System.out.println("\n--- DEMONSTRATIVO FINANCEIRO DE VENDAS ---");
        System.out.println("Quantidade de vendas: " + histVendas.size());
        System.out.println("Receita bruta em vendas: " + formatarMoeda(totalBruto));
        System.out.println("Total de impostos apurados: " + formatarMoeda(totalImposto));
        System.out.println("Lucro liquido acumulado: " + formatarMoeda(totalLucro));
        System.out.printf(LOCALE_PT_BR, "Margem liquida sobre vendas: %.2f%%%n", margemMedia);
        System.out.println("Saldo atual em caixa: " + formatarMoeda(saldoCaixa));
    }

    // =========================================================================
    // 9. MODULO TI E GERENCIA
    // =========================================================================

    private static void menuTi(Scanner sc) {
        while (true) {
            System.out.println("\n=== MODULO TI ===");
            System.out.println("1 - Desbloquear login de funcionario (apos 3 erros)");
            System.out.println("2 - Ajustar autoridade de funcionario (exige senha para gerencia; bloqueia inativo)");
            System.out.println("3 - Consultar historico de acessos e utilizacao");
            System.out.println("4 - Consultar arquivo .TXT salvo");
            System.out.println("5 - Gerar backup manual agora");
            System.out.println("0 - Voltar ao painel");
            System.out.print("Escolha: ");

            String op = sc.nextLine().trim();
            switch (op) {
                case "1":
                    desbloquearFuncionarioTi(sc);
                    break;
                case "2":
                    ajustarAutoridadeTi(sc);
                    break;
                case "3":
                    exibirListaPaginada(sc, "HISTORICO DE ACESSOS", histAcessos, 10, 0);
                    break;
                case "4":
                    consultarArquivoTxtSalvo(sc);
                    break;
                case "5":
                    salvarTodosArquivosTxtEBackup();
                    registrarAcesso("BACKUP_MANUAL_TI", "Backup manual executado", "OK");
                    salvarTodosArquivosTxtEBackup();
                    System.out.println("[OK] Backup manual salvo.");
                    break;
                case "0":
                    return;
                default:
                    System.out.println("[AVISO] Opcao invalida.");
                    break;
            }
        }
    }

    private static void desbloquearFuncionarioTi(Scanner sc) {
        String idFunc = normalizarTextoObrigatorio(sc, "ID do funcionario para desbloquear", true);
        if (idFunc == null) return;
        int idx = encontrarIndicePorChave(funcionarios, 0, idFunc);
        if (idx == -1) {
            System.out.println("[AVISO] Funcionario nao encontrado.");
            return;
        }
        String[] f = partirLinha(funcionarios.get(idx));
        f[8] = "false";
        f[11] = "0";
        funcionarios.set(idx, montarLinha(f));
        registrarAcesso("DESBLOQUEIO_LOGIN_TI", f[0], "OK");
        salvarTodosArquivosTxtEBackup();
        System.out.println("[OK] Conta " + f[0] + " desbloqueada.");
    }

    private static void ajustarAutoridadeTi(Scanner sc) {
        String idFunc = normalizarTextoObrigatorio(sc, "ID do funcionario para ajustar autoridade", true);
        if (idFunc == null) return;
        int idx = encontrarIndicePorChave(funcionarios, 0, idFunc);
        if (idx == -1) {
            System.out.println("[AVISO] Funcionario nao encontrado.");
            return;
        }
        String[] f = partirLinha(funcionarios.get(idx));
        boolean ativo = Boolean.parseBoolean(f[9]);
        if (!ativo) {
            System.out.println("[AVISO] Funcionario esta inativo/demitido. Autoridade nao pode ser validada/alterada.");
            return;
        }

        String autAtual = f.length > 13 ? f[13] : f[5];
        if ("GERENCIA".equalsIgnoreCase(f[5]) || "GERENCIA".equalsIgnoreCase(autAtual)) {
            System.out.print("Alvo e da GERENCIA: digite a senha de confirmacao do funcionario alvo: ");
            String senhaConf = sc.nextLine().trim();
            if (!f[6].equals(senhaConf)) {
                System.out.println("[BLOQUEADO] Senha de confirmacao da gerencia incorreta.");
                return;
            }
        }

        String novaAut = normalizarTextoObrigatorio(sc,
                "Nova autoridade (FUNCIONARIO, ESTOQUE, CAIXA, FINANCEIRO, RH, TI, GERENCIA)", true);
        if (novaAut == null) return;

        String autUpper = novaAut.toUpperCase(Locale.ROOT);
        f[13] = autUpper;
        funcionarios.set(idx, montarLinha(f));
        registrarAcesso("AJUSTE_AUTORIDADE_TI", f[0] + " -> " + autUpper, "OK");
        salvarTodosArquivosTxtEBackup();
        System.out.println("[OK] Autoridade de " + f[0] + " atualizada para " + autUpper + ".");
    }

    private static void consultarArquivoTxtSalvo(Scanner sc) {
        System.out.println("\nArquivos disponiveis:");
        System.out.println("1 - " + ARQ_FUNCIONARIOS);
        System.out.println("2 - " + ARQ_CLIENTES);
        System.out.println("3 - " + ARQ_FORNECEDORES);
        System.out.println("4 - " + ARQ_PRODUTOS);
        System.out.println("5 - " + ARQ_FINANCEIRO);
        System.out.println("6 - " + ARQ_VENDAS);
        System.out.println("7 - " + ARQ_HIST_ESTOQUE);
        System.out.println("8 - " + ARQ_HIST_ACESSOS);
        System.out.println("9 - " + ARQ_BACKUP);
        System.out.print("Escolha: ");
        String op = sc.nextLine().trim();

        String nomeArq;
        switch (op) {
            case "1": nomeArq = ARQ_FUNCIONARIOS; break;
            case "2": nomeArq = ARQ_CLIENTES; break;
            case "3": nomeArq = ARQ_FORNECEDORES; break;
            case "4": nomeArq = ARQ_PRODUTOS; break;
            case "5": nomeArq = ARQ_FINANCEIRO; break;
            case "6": nomeArq = ARQ_VENDAS; break;
            case "7": nomeArq = ARQ_HIST_ESTOQUE; break;
            case "8": nomeArq = ARQ_HIST_ACESSOS; break;
            case "9": nomeArq = ARQ_BACKUP; break;
            default:
                System.out.println("[AVISO] Opcao invalida.");
                return;
        }

        Path p = Paths.get(nomeArq);
        if (!Files.exists(p)) {
            System.out.println("[AVISO] Arquivo nao encontrado: " + nomeArq);
            return;
        }
        try {
            List<String> conteudo = Files.readAllLines(p, StandardCharsets.UTF_8);
            exibirListaPaginada(sc, "CONTEUDO DE " + nomeArq, conteudo, 15, 0);
        } catch (IOException e) {
            System.out.println("[ERRO] Falha ao ler " + nomeArq + ": " + e.getMessage());
        }
    }

    private static void menuGerencia(Scanner sc) {
        while (true) {
            System.out.println("\n=== PAINEL DE GERENCIA ===");
            System.out.println("Funcionarios: " + funcionarios.size() + " | Clientes: " + clientes.size()
                    + " | Fornecedores: " + fornecedores.size() + " | Produtos: " + produtos.size());
            System.out.println("Vendas: " + histVendas.size() + " | Saldo em Caixa: " + formatarMoeda(saldoCaixa));
            System.out.println("1 - Consultar vendas / notas fiscais");
            System.out.println("2 - Consultar fornecedores");
            System.out.println("3 - Consultar clientes");
            System.out.println("4 - Demonstrativo financeiro de lucro");
            System.out.println("0 - Voltar ao painel");
            System.out.print("Escolha: ");

            String op = sc.nextLine().trim();
            switch (op) {
                case "1":
                    exibirListaPaginada(sc, "VENDAS E NOTAS FISCAIS", histVendas, 10, 0);
                    break;
                case "2":
                    exibirListaPaginada(sc, "FORNECEDORES CADASTRADOS", fornecedores, 10, 0);
                    break;
                case "3":
                    exibirListaPaginada(sc, "CLIENTES CADASTRADOS", clientes, 10, 0);
                    break;
                case "4":
                    exibirDemonstrativoLucro();
                    break;
                case "0":
                    return;
                default:
                    System.out.println("[AVISO] Opcao invalida.");
                    break;
            }
        }
    }

    // =========================================================================
    // 10. CAIXA / VENDA DIRETA
    // =========================================================================

    private static void registrarVendaSimples(Scanner sc) {
        if (produtos.isEmpty() || clientes.isEmpty()) {
            System.out.println("[AVISO] Sem produtos ou clientes cadastrados.");
            return;
        }
        String codPrd = normalizarTextoObrigatorio(sc, "Codigo do produto (ex.: PRD-001)", true);
        if (codPrd == null) return;

        Integer qtd = lerInteiroSeguro(sc, "Quantidade", 1, null, true);
        if (qtd == null) return;

        int idxPrd = encontrarIndicePorChave(produtos, 0, codPrd);
        if (idxPrd == -1) {
            System.out.println("[AVISO] Produto nao encontrado.");
            return;
        }

        String[] p = partirLinha(produtos.get(idxPrd));
        if (p.length < 9 || !Boolean.parseBoolean(p[8])) {
            System.out.println("[AVISO] Produto inativo ou malformado.");
            return;
        }

        String nomePrd = p[1];
        String forId = p[2];
        double custoUnit = Double.parseDouble(p[3].trim());
        double precoUnit = Double.parseDouble(p[4].trim());
        int estoqueAtual = Integer.parseInt(p[5].trim());
        double aliq = Double.parseDouble(p[6].trim());

        if (estoqueAtual < qtd) {
            System.out.println("[AVISO] Estoque insuficiente. Disponivel: " + estoqueAtual);
            return;
        }

        int novoEstoque = estoqueAtual - qtd;
        p[5] = String.valueOf(novoEstoque);
        produtos.set(idxPrd, montarLinha(p));

        double subtotal = Math.round(precoUnit * qtd * 100.0) / 100.0;
        double imposto = Math.round(subtotal * (aliq / 100.0) * 100.0) / 100.0;
        double custoTotal = Math.round(custoUnit * qtd * 100.0) / 100.0;
        double lucroLiq = Math.round((subtotal - custoTotal - imposto) * 100.0) / 100.0;
        double totalNf = Math.round((subtotal + imposto) * 100.0) / 100.0;

        String[] cli = partirLinha(clientes.get(0));
        int nfNum = proxNf++;
        String nfStr = "NF-" + nfNum;
        String linhaVenda = montarLinha(
                nfStr, dataHojeFormatada(), cli[1], cli[2], p[0] + "x" + qtd,
                String.format(Locale.US, "%.2f", subtotal),
                String.format(Locale.US, "%.2f", imposto),
                String.format(Locale.US, "%.2f", custoTotal),
                String.format(Locale.US, "%.2f", lucroLiq),
                String.format(Locale.US, "%.2f", totalNf)
        );
        histVendas.add(linhaVenda);

        registrarMovEstoque("SAIDA", p[0], nomePrd, forId, qtd, "Venda no caixa " + nfStr);
        registrarLancamentoFinanceiro("VENDA", "Venda " + nfStr, totalNf);
        registrarAcesso("REGISTRO_VENDA_CAIXA", nfStr + " (" + p[0] + "x" + qtd + ")", "OK");
        salvarTodosArquivosTxtEBackup();

        System.out.println("[OK] Venda " + nfStr + " registrada! Total NF: "
                + formatarMoeda(totalNf) + " | Novo estoque: " + novoEstoque);
    }
}