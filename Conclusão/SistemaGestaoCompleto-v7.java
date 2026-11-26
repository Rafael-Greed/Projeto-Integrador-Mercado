import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.Set;


class SistemaGestaoCompleto {

    private static final String ARQ_BACKUP = "backup_sistema.txt";
    private static final String ARQ_FUNCIONARIOS = "txt_funcionarios.txt";
    private static final String ARQ_CLIENTES = "txt_clientes.txt";
    private static final String ARQ_FORNECEDORES = "txt_fornecedores.txt";
    private static final String ARQ_PRODUTOS = "txt_produtos.txt";
    private static final String ARQ_FINANCEIRO = "txt_hist_financeiro.txt";
    private static final String ARQ_VENDAS = "txt_hist_vendas.txt";
    private static final String ARQ_HIST_ESTOQUE = "txt_hist_estoque.txt";
    private static final String ARQ_HIST_ACESSOS = "txt_hist_acessos.txt";
    private static final String PASTA_QUARENTENA = "quarentena";

    private static double saldoCaixa = 100000.00;
    private static double aliquotaAnoAtual = 5.5;
    private static double aliquotaProximoAno = 7.0;
    private static int proxNf = 1001;
    private static int proxFin = 2;
    private static int proxMovEst = 2;
    private static int proxAcesso = 2;

    // Formato de cada linha (separada por ';'):
    // funcionarios: id;nome;cpf;dataNasc;sexo;cargo;senha;salario;pago;ativo;bloqueado;tentativasFalhas;motivoDemissao;autoridadesCsv
    // clientes: id;cpf;nome
    // fornecedores: id;nome;cnpj;uf;produtoVinculadoId;valorCompraPadrao
    // produtos: id;nome;fornecedorId;custoUnit;precoVenda;estoque;aliqAtual;aliqProx;ativo
    // histFinanceiro: id;data;tipo;descricao;valor;saldoApos
    // histVendas: nf;data;cpfCliente;nomeCliente;itensQtd;subtotal;imposto;custoTotal;lucroLiq;totalNf
    // histEstoque: id;data;tipoMov;prdId;nomePrd;forId;qtd;usuarioId;obs
    // histAcessos: id;dataHora;usuarioId;cargo;evento;detalhe;status
    // carrinhoCliente: prdId;nomePrd;forId;qtd;precoUnit;custoUnit;aliq
    private static final List<String> funcionarios = new ArrayList<>();
    private static final List<String> clientes = new ArrayList<>();
    private static final List<String> fornecedores = new ArrayList<>();
    private static final List<String> produtos = new ArrayList<>();
    private static final List<String> histFinanceiro = new ArrayList<>();
    private static final List<String> histVendas = new ArrayList<>();
    private static final List<String> histEstoque = new ArrayList<>();
    private static final List<String> histAcessos = new ArrayList<>();
    private static final List<String> carrinhoCliente = new ArrayList<>();

    private static String usuarioLogadoId = null;
    private static String usuarioLogadoNome = null;
    private static String usuarioLogadoCargo = null;
    private static String usuarioLogadoAutoridades = "";

    private static final DateTimeFormatter FMT_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public static void main(String[] args) {
        System.out.println("=== INICIALIZANDO SISTEMA DE GESTAO ===");
        inicializarSistemaNaAbertura();
        executarMenuPrincipal();
    }

    public static void inicializarSistemaNaAbertura() {
        Path pathBackup = Paths.get(ARQ_BACKUP);

        if (!Files.exists(pathBackup)) {
            System.out.println("[AVISO] Arquivo " + ARQ_BACKUP + " nao encontrado.");
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
            System.out.println("[INFO] Sistema aberto sem dados inconsistentes carregados (base padrao ativa).");
            return;
        }

        carregarDadosDoBackup(linhasBackup);
        sincronizarArquivosTxtComMemoria();
        registrarAcesso("LOGIN_BACKUP_OK", "DADOS_CARREGADOS_E_SINCRONIZADOS", "OK");
        System.out.println("[OK] Backup validado e todos os .TXT sincronizados com sucesso.");
    }

    public static String validarConsistenciaBackup(List<String> linhas) {
        if (linhas.isEmpty()) {
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

        Set<String> idsProdutosValidos = new HashSet<>();
        double ultimoSaldoFinanceiro = Double.NaN;
        int linhasFinanceiras = 0;

        String secaoAtual = "";
        for (int i = 0; i < linhas.size(); i++) {
            String linha = linhas.get(i).trim();
            if (linha.isEmpty()) {
                continue;
            }
            if (linha.startsWith("[") && linha.endsWith("]")) {
                secaoAtual = linha;
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

            String[] partes = linha.split(";", -1);
            if ("[CONTROLE]".equals(secaoAtual)) {
                if (partes.length < 2) return "Linha invalida em [CONTROLE]: " + linha;
                if ("SALDO".equals(partes[0])) {
                    try {
                        Double.parseDouble(partes[1]);
                    } catch (NumberFormatException e) {
                        return "SALDO invalido em [CONTROLE]";
                    }
                } else if ("PROX_NF".equals(partes[0])) {
                    try {
                        Integer.parseInt(partes[1]);
                    } catch (NumberFormatException e) {
                        return "PROX_NF invalido em [CONTROLE]";
                    }
                }
            } else if ("[PRODUTOS]".equals(secaoAtual)) {
                if (partes.length < 9) return "Campos insuficientes em [PRODUTOS] na linha " + (i + 1);
                try {
                    double custo = Double.parseDouble(partes[3]);
                    double preco = Double.parseDouble(partes[4]);
                    int estoque = Integer.parseInt(partes[5]);
                    if (custo < 0 || preco < 0 || estoque < 0) {
                        return "Valores negativos em [PRODUTOS] (" + partes[0] + ")";
                    }
                } catch (NumberFormatException e) {
                    return "Numero invalido em [PRODUTOS] (" + partes[0] + ")";
                }
                idsProdutosValidos.add(partes[0]);
            } else if ("[VENDAS]".equals(secaoAtual)) {
                if (partes.length < 10) return "Campos insuficientes em [VENDAS] na linha " + (i + 1);
                String itemQtd = partes[4];
                String codPrd = itemQtd.contains("x") ? itemQtd.substring(0, itemQtd.indexOf('x')) : itemQtd;
                if (!idsProdutosValidos.isEmpty() && !idsProdutosValidos.contains(codPrd)) {
                    return "Venda " + partes[0] + " referencia produto inexistente " + codPrd;
                }
                try {
                    double totalNf = Double.parseDouble(partes[9]);
                    if (totalNf < 0) return "Total de NF negativo em " + partes[0];
                } catch (NumberFormatException e) {
                    return "Valor numerico invalido em [VENDAS] (" + partes[0] + ")";
                }
            } else if ("[FINANCEIRO]".equals(secaoAtual)) {
                if (partes.length < 6) return "Campos insuficientes em [FINANCEIRO] na linha " + (i + 1);
                try {
                    Double.parseDouble(partes[4]);
                    ultimoSaldoFinanceiro = Double.parseDouble(partes[5]);
                    linhasFinanceiras++;
                } catch (NumberFormatException e) {
                    return "Valor financeiro invalido em " + partes[0];
                }
            } else if ("[HIST_ESTOQUE]".equals(secaoAtual)) {
                if (partes.length < 9) return "Campos insuficientes em [HIST_ESTOQUE] na linha " + (i + 1);
            } else if ("[HIST_ACESSOS]".equals(secaoAtual)) {
                if (partes.length < 7) return "Campos insuficientes em [HIST_ACESSOS] na linha " + (i + 1);
            }
        }

        if (!temControle || !temFuncionarios || !temClientes || !temFornecedores || !temProdutos
                || !temFinanceiro || !temVendas || !temHistEstoque || !temHistAcessos) {
            return "Uma ou mais secoes obrigatorias estao ausentes no backup";
        }

        double saldoControle = extrairSaldoControle(linhas);
        if (linhasFinanceiras > 0 && !Double.isNaN(ultimoSaldoFinanceiro)
                && Math.abs(saldoControle - ultimoSaldoFinanceiro) > 0.05) {
            return "Divergencia entre SALDO de controle (" + saldoControle
                    + ") e saldo final de [FINANCEIRO] (" + ultimoSaldoFinanceiro + ")";
        }

        return null;
    }

    private static double extrairSaldoControle(List<String> linhas) {
        boolean emControle = false;
        for (String linha : linhas) {
            String l = linha.trim();
            if (l.startsWith("[") && l.endsWith("]")) {
                emControle = "[CONTROLE]".equals(l);
                continue;
            }
            if (emControle && l.startsWith("SALDO;")) {
                String[] p = l.split(";", -1);
                return Double.parseDouble(p[1]);
            }
        }
        return saldoCaixa;
    }

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
            String linha = raw.trim();
            if (linha.isEmpty()) continue;
            if (linha.startsWith("[") && linha.endsWith("]")) {
                secao = linha;
                continue;
            }
            switch (secao) {
                case "[CONTROLE]":
                    String[] p = linha.split(";", -1);
                    if (p.length >= 2 && "SALDO".equals(p[0])) {
                        saldoCaixa = Double.parseDouble(p[1]);
                    } else if (p.length >= 2 && "PROX_NF".equals(p[0])) {
                        proxNf = Integer.parseInt(p[1]);
                    } else if (p.length >= 2 && "ALIQ_ATUAL".equals(p[0])) {
                        aliquotaAnoAtual = Double.parseDouble(p[1]);
                    } else if (p.length >= 2 && "ALIQ_PROX".equals(p[0])) {
                        aliquotaProximoAno = Double.parseDouble(p[1]);
                    }
                    break;
                case "[FUNCIONARIOS]":
                    funcionarios.add(normalizarLinhaFuncionario(linha));
                    break;
                case "[CLIENTES]":
                    clientes.add(linha);
                    break;
                case "[FORNECEDORES]":
                    fornecedores.add(linha);
                    break;
                case "[PRODUTOS]":
                    produtos.add(linha);
                    break;
                case "[FINANCEIRO]":
                    histFinanceiro.add(linha);
                    proxFin = histFinanceiro.size() + 1;
                    break;
                case "[VENDAS]":
                    histVendas.add(linha);
                    break;
                case "[HIST_ESTOQUE]":
                    histEstoque.add(linha);
                    proxMovEst = histEstoque.size() + 1;
                    break;
                case "[HIST_ACESSOS]":
                    histAcessos.add(linha);
                    proxAcesso = histAcessos.size() + 1;
                    break;
                default:
                    break;
            }
        }
    }

    private static String normalizarLinhaFuncionario(String linha) {
        String[] p = partirLinha(linha);
        // Garante 14 campos:
        // 0:id; 1:nome; 2:cpf; 3:dataNasc; 4:sexo; 5:cargo; 6:senha; 7:salario;
        // 8:pago; 9:ativo; 10:bloqueado; 11:tentativasFalhas; 12:motivoDemissao; 13:autoridadesCsv
        String id = p.length > 0 ? p[0] : "UR-FUNC01";
        String nome = p.length > 1 ? p[1] : "FUNCIONARIO";
        String cpf = p.length > 2 ? p[2] : "11144477735";
        String nasc = p.length > 3 ? p[3] : "02/02/1981";
        String sexo = p.length > 4 ? p[4] : "F";
        String cargo = p.length > 5 ? p[5].toUpperCase(Locale.ROOT) : "ESTOQUE";
        String senha = p.length > 6 ? p[6] : "senha01";
        String sal = p.length > 7 ? p[7] : "2500.0";
        String pago = p.length > 8 ? p[8] : "false";
        String ativo = p.length > 9 ? p[9] : "true";
        String bloq = p.length > 10 ? p[10] : "false";
        String tent = p.length > 11 ? p[11] : "0";
        String motivo = p.length > 12 ? p[12] : "N/A";
        String aut = (p.length > 13 && !p[13].trim().isEmpty())
                ? p[13].trim().toUpperCase(Locale.ROOT)
                : autoridadesPadraoPorCargo(cargo);
        return montarLinha(id, nome, cpf, nasc, sexo, cargo, senha, sal, pago, ativo, bloq, tent, motivo, aut);
    }

    private static String autoridadesPadraoPorCargo(String cargo) {
        switch (cargo == null ? "" : cargo.toUpperCase(Locale.ROOT)) {
            case "GERENCIA":
                return "PERFIL,ESTOQUE,GERENCIA,RH,CAIXA,FINANCEIRO,TI";
            case "RH":
                return "PERFIL,RH";
            case "ESTOQUE":
                return "PERFIL,ESTOQUE";
            case "CAIXA":
                return "PERFIL,CAIXA";
            case "FINANCEIRO":
                return "PERFIL,FINANCEIRO";
            case "TI":
                return "PERFIL,TI";
            default:
                return "PERFIL";
        }
    }

    public static void enviarParaQuarentena(Path pathBackup, String motivo) {
        try {
            Path dirQuarentena = Paths.get(PASTA_QUARENTENA);
            Files.createDirectories(dirQuarentena);
            String sufixo = "_quarentena_" + System.currentTimeMillis() + ".txt";

            if (Files.exists(pathBackup)) {
                Files.move(pathBackup, dirQuarentena.resolve(ARQ_BACKUP + sufixo), StandardCopyOption.REPLACE_EXISTING);
            }

            String[] arquivosTxt = {
                ARQ_FUNCIONARIOS, ARQ_CLIENTES, ARQ_FORNECEDORES, ARQ_PRODUTOS,
                ARQ_FINANCEIRO, ARQ_VENDAS, ARQ_HIST_ESTOQUE, ARQ_HIST_ACESSOS
            };
            for (String nomeTxt : arquivosTxt) {
                Path p = Paths.get(nomeTxt);
                if (Files.exists(p)) {
                    Files.move(p, dirQuarentena.resolve(nomeTxt + sufixo), StandardCopyOption.REPLACE_EXISTING);
                }
            }

            Path logQuarentena = dirQuarentena.resolve("motivo_quarentena.log");
            String registro = "DataHora=" + new Date() + " | Motivo=" + motivo + System.lineSeparator();
            Files.write(logQuarentena, registro.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.out.println("[ERRO] Falha ao mover arquivos para quarentena: " + e.getMessage());
        }
    }

    public static void carregarBasePadrao() {
        saldoCaixa = 100000.00;
        aliquotaAnoAtual = 5.5;
        aliquotaProximoAno = 7.0;
        proxNf = 1001;
        proxFin = 2;
        proxMovEst = 2;
        proxAcesso = 2;

        funcionarios.clear();
        funcionarios.add("UR-FUNC01;FUNCIONARIO TESTE 01;11144477735;02/02/1981;F;GERENCIA;senha01;3550.0;false;true;false;0;N/A;PERFIL,ESTOQUE,GERENCIA,RH,CAIXA,FINANCEIRO,TI");
        funcionarios.add("UR-FUNC02;MARIA RH;22255588846;15/05/1990;F;RH;rh123;3200.0;false;true;false;0;N/A;PERFIL,RH");
        funcionarios.add("UR-FUNC03;JOAO ESTOQUE;33366699957;10/10/1995;M;ESTOQUE;est123;2600.0;false;true;false;0;N/A;PERFIL,ESTOQUE");
        funcionarios.add("UR-FUNC04;ANA CAIXA;44477788868;20/07/1998;F;CAIXA;cxs123;2400.0;false;true;false;0;N/A;PERFIL,CAIXA");
        funcionarios.add("UR-FUNC05;CARLOS FINANC;55588899979;03/03/1988;M;FINANCEIRO;fin123;4100.0;false;true;false;0;N/A;PERFIL,FINANCEIRO");
        funcionarios.add("UR-FUNC06;LUCAS TI;66699911180;12/12/1992;M;TI;ti123;3900.0;false;true;false;0;N/A;PERFIL,TI");

        clientes.clear();
        clientes.add("CLI-001;11122233301;CLIENTE GESTAO 001");

        fornecedores.clear();
        fornecedores.add("FOR-001;FORNECEDOR PARCEIRO 001 LTDA;10000000123456;SP;PRD-001;11.45");

        produtos.clear();
        produtos.add("PRD-001;PRODUTO GESTAO ITEM 001;FOR-001;11.45;17.96;21;5.5;7.0;true");

        histFinanceiro.clear();
        histFinanceiro.add("FIN-0001;" + dataHojeFormatada() + ";DEPOSITO_INICIAL;Abertura de caixa inicial;100000.00;100000.00");

        histVendas.clear();

        histEstoque.clear();
        histEstoque.add("MOV-0001;" + dataHojeFormatada() + ";ENTRADA;PRD-001;PRODUTO GESTAO ITEM 001;FOR-001;21;UR-FUNC01;Carga inicial base padrao");

        histAcessos.clear();
        histAcessos.add("ACC-0001;" + dataHoraAgoraFormatada() + ";UR-FUNC01;GERENCIA;LOGIN_SUCESSO;MENU_PRINCIPAL;OK");
    }

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
        backupLinhas.add(String.format(Locale.US, "SALDO;%.2f", saldoCaixa));
        backupLinhas.add("PROX_NF;" + proxNf);
        backupLinhas.add(String.format(Locale.US, "ALIQ_ATUAL;%.2f", aliquotaAnoAtual));
        backupLinhas.add(String.format(Locale.US, "ALIQ_PROX;%.2f", aliquotaProximoAno));

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
        try {
            return Files.readAllLines(caminho, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    private static void escreverLinhas(Path caminho, List<String> linhas) {
        try {
            Files.write(caminho, linhas, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.out.println("[ERRO] Falha ao gravar " + caminho.getFileName() + ": " + e.getMessage());
        }
    }

    public static void registrarAcesso(String evento, String detalhe, String status) {
        String id = String.format("ACC-%04d", proxAcesso++);
        String ator = usuarioLogadoId != null ? usuarioLogadoId : "VISITANTE";
        String cargo = usuarioLogadoCargo != null ? usuarioLogadoCargo : "CLIENTE/PUBLICO";
        String linha = montarLinha(id, dataHoraAgoraFormatada(), ator, cargo, evento, detalhe, status);
        histAcessos.add(linha);
    }

    public static void registrarMovEstoque(String tipoMov, String prdId, String nomePrd, String forId, int qtd, String obs) {
        String id = String.format("MOV-%04d", proxMovEst++);
        String ator = usuarioLogadoId != null ? usuarioLogadoId : "CLIENTE";
        String linha = montarLinha(id, dataHojeFormatada(), tipoMov, prdId, nomePrd,
                forId, String.valueOf(qtd), ator, obs);
        histEstoque.add(linha);
    }

    public static void registrarLancamentoFinanceiro(String tipo, String descricao, double valor) {
        saldoCaixa = Math.round((saldoCaixa + valor) * 100.0) / 100.0;
        String id = String.format(Locale.US, "FIN-%04d", proxFin++);
        String linha = String.format(Locale.US, "%s;%s;%s;%s;%.2f;%.2f",
                id, dataHojeFormatada(), tipo, descricao, valor, saldoCaixa);
        histFinanceiro.add(linha);
    }

    // ==================== MENU PRINCIPAL E AUTENTICACAO ====================

    public static void executarMenuPrincipal() {
        Scanner sc = new Scanner(System.in);
        boolean rodando = true;

        while (rodando) {
            deslogarSeNecessario();
            System.out.println("\n================ TELA INICIAL ================");
            System.out.println("Saldo atual: " + formatarMoeda(saldoCaixa)
                    + " | Aliq Atual: " + aliquotaAnoAtual + "% | Aliq Prox Ano: " + aliquotaProximoAno + "%");
            System.out.println("1 - Acessar como Cliente (Vitrine / Carrinho / Compra)");
            System.out.println("2 - Login de Funcionario (Meu Perfil / Estoque / RH / Caixa / Financeiro / TI / Gerencia)");
            System.out.println("3 - Salvar backup manual (.TXT) e sincronizar arquivos");
            System.out.println("0 - Encerrar sistema (salva backup automatico)");
            System.out.print("Escolha: ");

            String opcao = sc.nextLine().trim();
            switch (opcao) {
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

    private static void deslogarSeNecessario() {
        usuarioLogadoId = null;
        usuarioLogadoNome = null;
        usuarioLogadoCargo = null;
        usuarioLogadoAutoridades = "";
    }

    private static boolean realizarLoginFuncionario(Scanner sc) {
        System.out.println("\n=== LOGIN DE FUNCIONARIO ===");
        System.out.print("ID ou CPF do funcionario (ou CANCELAR): ");
        String ident = sc.nextLine().trim();
        if (ident.isEmpty() || "CANCELAR".equalsIgnoreCase(ident)) {
            return false;
        }

        int idx = encontrarIndiceFuncionario(ident);
        if (idx == -1) {
            System.out.println("[AVISO] Funcionario nao encontrado.");
            registrarAcesso("LOGIN_FALHA", "IDENTIFICADOR_INEXISTENTE:" + ident, "NEGADO");
            return false;
        }

        String[] f = partirLinha(funcionarios.get(idx));
        String id = f[0];
        String nome = f[1];
        String cargo = f[5].toUpperCase(Locale.ROOT);
        String senha = f[6];
        boolean ativo = "true".equalsIgnoreCase(f[9]);
        boolean bloqueado = "true".equalsIgnoreCase(f[10]);
        int tentativas = lerIntSafe(f[11], 0);

        if (!ativo) {
            System.out.println("[AVISO] Usuario desativado/demitido. Contate RH/TI.");
            registrarAcesso("LOGIN_BLOQUEADO_INATIVO", id, "NEGADO");
            return false;
        }

        if (bloqueado) {
            System.out.println("[ALERTA] Login bloqueado apos 3 tentativas incorretas. Solicite desbloqueio a TI.");
            registrarAcesso("LOGIN_BLOQUEADO", id, "NEGADO");
            return false;
        }

        System.out.print("Senha: ");
        String senhaDigitada = sc.nextLine().trim();

        if ("GERENCIA".equals(cargo)) {
            System.out.print("ID de Autoridade da Gerencia: ");
            String idAut = sc.nextLine().trim();
            if (!id.equalsIgnoreCase(idAut)) {
                tentativas++;
                f[11] = String.valueOf(tentativas);
                if (tentativas >= 3) {
                    f[10] = "true";
                }
                funcionarios.set(idx, montarLinha(f));
                salvarTodosArquivosTxtEBackup();
                System.out.println("[AVISO] ID de autoridade da Gerencia invalido. Tentativa " + tentativas + "/3.");
                registrarAcesso("LOGIN_GERENCIA_FALHA", id + " (ID aut invalido)", "NEGADO");
                return false;
            }
        }

        if (!senha.equals(senhaDigitada)) {
            tentativas++;
            f[11] = String.valueOf(tentativas);
            if (tentativas >= 3) {
                f[10] = "true";
                System.out.println("[ALERTA] 3 tentativas incorretas! Conta bloqueada.");
            } else {
                System.out.println("[AVISO] Senha incorreta. Tentativa " + tentativas + "/3.");
            }
            funcionarios.set(idx, montarLinha(f));
            salvarTodosArquivosTxtEBackup();
            registrarAcesso("LOGIN_SENHA_FALHA", id + " (tentativa " + tentativas + "/3)", "NEGADO");
            return false;
        }

        f[11] = "0";
        funcionarios.set(idx, montarLinha(f));
        usuarioLogadoId = id;
        usuarioLogadoNome = nome;
        usuarioLogadoCargo = cargo;
        usuarioLogadoAutoridades = f.length > 13 ? f[13] : autoridadesPadraoPorCargo(cargo);
        registrarAcesso("LOGIN_SUCESSO", "CARGO=" + cargo, "OK");
        salvarTodosArquivosTxtEBackup();
        System.out.println("[OK] Bem-vindo(a), " + nome + " (" + cargo + ")!");
        return true;
    }

    private static boolean possuiAutoridade(String modulo) {
        if ("GERENCIA".equalsIgnoreCase(usuarioLogadoCargo)) {
            return true;
        }
        String[] auts = usuarioLogadoAutoridades.split(",");
        for (String a : auts) {
            if (a.trim().equalsIgnoreCase(modulo)) {
                return true;
            }
        }
        return false;
    }

    private static void menuFuncionarioAutenticado(Scanner sc) {
        while (true) {
            System.out.println("\n=== MENU DO FUNCIONARIO ===");
            System.out.println("Usuario: " + usuarioLogadoNome + " (" + usuarioLogadoId + ") | Cargo: "
                    + usuarioLogadoCargo + " | Saldo: " + formatarMoeda(saldoCaixa));

            List<String> opcoes = new ArrayList<>();
            List<String> chaves = new ArrayList<>();

            if (possuiAutoridade("PERFIL")) {
                chaves.add("PERFIL");
                opcoes.add("Meu Perfil (dados do usuario logado)");
            }
            if (possuiAutoridade("ESTOQUE")) {
                chaves.add("ESTOQUE");
                opcoes.add("Estoque e Fornecedores (produtos, movimentacoes, compras e edicao modal)");
            }
            if (possuiAutoridade("RH")) {
                chaves.add("RH");
                opcoes.add("RH (contratar, cadastrar, demitir com motivo, readmitir e editar modal)");
            }
            if (possuiAutoridade("CAIXA")) {
                chaves.add("CAIXA");
                opcoes.add("Caixa (vender produto e emitir Nota Fiscal)");
            }
            if (possuiAutoridade("FINANCEIRO")) {
                chaves.add("FINANCEIRO");
                opcoes.add("Financeiro (salarios, fornecedores, aliquotas ano atual/proximo e historico)");
            }
            if (possuiAutoridade("TI")) {
                chaves.add("TI");
                opcoes.add("TI (desbloquear login, ajustar autoridades, historico acessos, backup e .TXT)");
            }
            if (possuiAutoridade("GERENCIA")) {
                chaves.add("GERENCIA");
                opcoes.add("Gerencia (visao geral de todo o sistema e historicos)");
            }

            for (int i = 0; i < opcoes.size(); i++) {
                System.out.println((i + 1) + " - " + opcoes.get(i));
            }
            System.out.println("0 - Sair / Voltar ao inicio (desloga usuario)");
            System.out.print("Escolha: ");

            String entrada = sc.nextLine().trim();
            if ("0".equals(entrada)) {
                registrarAcesso("LOGOUT_FUNCIONARIO", usuarioLogadoId, "OK");
                salvarTodosArquivosTxtEBackup();
                deslogarSeNecessario();
                return;
            }

            int idxOp = lerIntSafe(entrada, -1) - 1;
            if (idxOp < 0 || idxOp >= chaves.size()) {
                System.out.println("[AVISO] Opcao invalida ou sem autoridade.");
                continue;
            }

            String chave = chaves.get(idxOp);
            switch (chave) {
                case "PERFIL":
                    exibirMeuPerfil();
                    break;
                case "ESTOQUE":
                    menuEstoqueEFornecedores(sc);
                    break;
                case "RH":
                    menuRh(sc);
                    break;
                case "CAIXA":
                    menuCaixa(sc);
                    break;
                case "FINANCEIRO":
                    menuFinanceiro(sc);
                    break;
                case "TI":
                    menuTi(sc);
                    break;
                case "GERENCIA":
                    menuGerencia(sc);
                    break;
                default:
                    break;
            }
        }
    }

    // ==================== 1. PORTAL DO CLIENTE ====================

    private static void menuCliente(Scanner sc) {
        String filtroAtual = "";
        while (true) {
            System.out.println("\n=== PORTAL DO CLIENTE ===");
            System.out.println("Filtro ativo: " + (filtroAtual.isEmpty() ? "[NENHUM]" : filtroAtual)
                    + " | Itens no carrinho: " + carrinhoCliente.size());
            System.out.println("1 - Vitrine de produtos (paginada ate 25 paginas, filtro e adicionar ao carrinho)");
            System.out.println("2 - Limpar filtro de pesquisa");
            System.out.println("3 - Carrinho e Compra (ver, adicionar, alterar qtd, remover e emitir NF)");
            System.out.println("0 - Voltar ao inicio");
            System.out.print("Escolha: ");

            String op = sc.nextLine().trim();
            switch (op) {
                case "1":
                    filtroAtual = listarProdutosClientePaginado(sc, filtroAtual);
                    break;
                case "2":
                    filtroAtual = "";
                    System.out.println("[OK] Filtro removido.");
                    break;
                case "3":
                    menuCarrinhoECompra(sc);
                    break;
                case "0":
                    return;
                default:
                    System.out.println("[AVISO] Opcao invalida.");
                    break;
            }
        }
    }

    private static String listarProdutosClientePaginado(Scanner sc, String filtroAtual) {
        String filtro = filtroAtual == null ? "" : filtroAtual.trim();
        int paginaAtual = 1;
        int itensPorPagina = 10;
        int maxPaginas = 25;

        while (true) {
            List<String> lista = obterProdutosAtivosFiltrados(filtro);
            if (lista.isEmpty()) {
                System.out.println("[AVISO] Nenhum produto encontrado para o filtro: " + filtro);
                System.out.print("[F] Novo filtro | [L] Limpar filtro | [C] Adicionar ao carrinho | [S] Sair: ");
                String acao = sc.nextLine().trim().toUpperCase(Locale.ROOT);
                if ("F".equals(acao)) {
                    System.out.print("Digite termo de busca (nome ou codigo): ");
                    filtro = sc.nextLine().trim();
                    paginaAtual = 1;
                } else if ("L".equals(acao)) {
                    filtro = "";
                    paginaAtual = 1;
                } else if ("C".equals(acao)) {
                    adicionarProdutoAoCarrinho(sc);
                } else if ("S".equals(acao) || acao.isEmpty()) {
                    return filtro;
                } else {
                    System.out.println("[AVISO] Comando invalido.");
                }
                continue;
            }

            int paginasCalculadas = (lista.size() + itensPorPagina - 1) / itensPorPagina;
            int totalPaginas = Math.min(paginasCalculadas, maxPaginas);
            int limiteItensExibiveis = Math.min(lista.size(), totalPaginas * itensPorPagina);
            paginaAtual = Math.max(1, Math.min(paginaAtual, totalPaginas));

            int inicio = (paginaAtual - 1) * itensPorPagina;
            int fim = Math.min(inicio + itensPorPagina, limiteItensExibiveis);

            System.out.println("\n=== VITRINE DE PRODUTOS (Pagina " + paginaAtual + " de " + totalPaginas
                    + " | Filtro: " + (filtro.isEmpty() ? "[NENHUM]" : filtro)
                    + " | Carrinho: " + carrinhoCliente.size() + " item(ns)) ===");
            for (int i = inicio; i < fim; i++) {
                String[] p = partirLinha(lista.get(i));
                double preco = lerDoubleSafe(p[4], 0.0);
                System.out.println((i + 1) + ". " + p[0] + " | " + p[1]
                        + " | Preco: " + formatarMoeda(preco)
                        + " | Estoque: " + p[5]
                        + " | Aliq: " + p[6] + "%"
                        + " | Fornecedor: " + p[2]);
            }

            System.out.print("[N] Prox | [A] Ant | [P] Pag | [F] Filtrar | [L] Limpar | [+] Add Carrinho | [V] Ver Carrinho | [S] Sair: ");
            String cmd = sc.nextLine().trim().toUpperCase(Locale.ROOT);
            if ("N".equals(cmd)) {
                if (paginaAtual < totalPaginas) paginaAtual++;
                else System.out.println("Voce ja esta na ultima pagina.");
            } else if ("A".equals(cmd)) {
                if (paginaAtual > 1) paginaAtual--;
                else System.out.println("Voce ja esta na primeira pagina.");
            } else if ("P".equals(cmd)) {
                System.out.print("Numero da pagina (1 a " + totalPaginas + "): ");
                int escolhida = lerIntSafe(sc.nextLine().trim(), -1);
                if (escolhida >= 1 && escolhida <= totalPaginas) paginaAtual = escolhida;
                else System.out.println("Pagina fora do intervalo valido.");
            } else if ("F".equals(cmd)) {
                System.out.print("Digite termo de busca (nome ou codigo): ");
                filtro = sc.nextLine().trim();
                paginaAtual = 1;
            } else if ("L".equals(cmd)) {
                filtro = "";
                paginaAtual = 1;
            } else if ("+".equals(cmd) || "ADD".equals(cmd) || "C".equals(cmd)) {
                adicionarProdutoAoCarrinho(sc);
            } else if ("V".equals(cmd)) {
                menuCarrinhoECompra(sc);
            } else if ("S".equals(cmd) || cmd.isEmpty()) {
                return filtro;
            } else {
                System.out.println("[AVISO] Comando invalido.");
            }
        }
    }

    private static void menuCarrinhoECompra(Scanner sc) {
        while (true) {
            System.out.println("\n=== CARRINHO E FINALIZACAO DE COMPRA ===");
            if (carrinhoCliente.isEmpty()) {
                System.out.println("Carrinho vazio.");
                System.out.println("1 - Adicionar produto ao carrinho");
                System.out.println("0 - Voltar");
                System.out.print("Escolha: ");
                String opVazio = sc.nextLine().trim();
                if ("1".equals(opVazio)) {
                    adicionarProdutoAoCarrinho(sc);
                } else if ("0".equals(opVazio)) {
                    return;
                } else {
                    System.out.println("[AVISO] Opcao invalida.");
                }
                continue;
            }

            double totalBruto = 0.0;
            double totalImpostosEstimados = 0.0;
            for (int i = 0; i < carrinhoCliente.size(); i++) {
                String[] c = partirLinha(carrinhoCliente.get(i));
                int qtd = lerIntSafe(c[3], 0);
                double unit = lerDoubleSafe(c[4], 0.0);
                double aliq = c.length > 6 ? lerDoubleSafe(c[6], aliquotaAnoAtual) : aliquotaAnoAtual;
                double sub = Math.round(qtd * unit * 100.0) / 100.0;
                double imp = Math.round(sub * (aliq / 100.0) * 100.0) / 100.0;
                totalBruto = Math.round((totalBruto + sub) * 100.0) / 100.0;
                totalImpostosEstimados = Math.round((totalImpostosEstimados + imp) * 100.0) / 100.0;
                System.out.println((i + 1) + ". [" + c[0] + "] " + c[1] + " | Qtd: " + qtd
                        + " | Unit: " + formatarMoeda(unit) + " | Subtotal: " + formatarMoeda(sub));
            }
            double totalEstimadoNf = Math.round((totalBruto + totalImpostosEstimados) * 100.0) / 100.0;
            System.out.println("Subtotal: " + formatarMoeda(totalBruto)
                    + " | Impostos estimados: " + formatarMoeda(totalImpostosEstimados)
                    + " | Total estimado NF: " + formatarMoeda(totalEstimadoNf));
            System.out.println("1 - Adicionar produto ao carrinho");
            System.out.println("2 - Alterar quantidade de um item");
            System.out.println("3 - Remover item do carrinho");
            System.out.println("4 - Finalizar compra e emitir Nota Fiscal (exige CPF e Nome)");
            System.out.println("0 - Voltar");
            System.out.print("Escolha: ");
            String op = sc.nextLine().trim();

            if ("1".equals(op)) {
                adicionarProdutoAoCarrinho(sc);
            } else if ("2".equals(op)) {
                String cod = normalizarTextoObrigatorio(sc, "Codigo do produto no carrinho", true);
                if (cod == null) continue;
                int idxCar = encontrarIndicePorChave(carrinhoCliente, 0, cod);
                if (idxCar == -1) {
                    System.out.println("[AVISO] Item nao encontrado no carrinho.");
                    continue;
                }
                int idxPrd = encontrarIndicePorChave(produtos, 0, cod);
                int estDisp = idxPrd >= 0 ? lerIntSafe(partirLinha(produtos.get(idxPrd))[5], 0) : 0;
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
            } else if ("3".equals(op)) {
                String cod = normalizarTextoObrigatorio(sc, "Codigo do produto para remover", true);
                if (cod == null) continue;
                int idxCar = encontrarIndicePorChave(carrinhoCliente, 0, cod);
                if (idxCar >= 0) {
                    carrinhoCliente.remove(idxCar);
                    System.out.println("[OK] Item removido do carrinho.");
                } else {
                    System.out.println("[AVISO] Item nao encontrado no carrinho.");
                }
            } else if ("4".equals(op)) {
                finalizarCompraCliente(sc);
                return;
            } else if ("0".equals(op)) {
                return;
            } else {
                System.out.println("[AVISO] Opcao invalida.");
            }
        }
    }

    private static void adicionarProdutoAoCarrinho(Scanner sc) {
        String cod = normalizarTextoObrigatorio(sc, "Codigo do produto (ou CANCELAR)", true);
        if (cod == null) return;
        int idxPrd = encontrarIndicePorChave(produtos, 0, cod);
        if (idxPrd == -1) {
            System.out.println("[AVISO] Produto nao encontrado.");
            return;
        }
        String[] p = partirLinha(produtos.get(idxPrd));
        if (p.length > 8 && !"true".equalsIgnoreCase(p[8])) {
            System.out.println("[AVISO] Produto inativo.");
            return;
        }
        int estoqueDisp = lerIntSafe(p[5], 0);
        if (estoqueDisp <= 0) {
            System.out.println("[AVISO] Produto sem estoque disponivel.");
            return;
        }
        Integer qtd = lerInteiroSeguro(sc, "Quantidade (Disponivel: " + estoqueDisp + ")", 1, estoqueDisp, true);
        if (qtd == null) return;

        int idxCar = encontrarIndicePorChave(carrinhoCliente, 0, p[0]);
        if (idxCar >= 0) {
            String[] c = partirLinha(carrinhoCliente.get(idxCar));
            int novaQtd = lerIntSafe(c[3], 0) + qtd;
            if (novaQtd > estoqueDisp) {
                System.out.println("[AVISO] Quantidade total excede o estoque disponivel (" + estoqueDisp + ").");
                return;
            }
            c[3] = String.valueOf(novaQtd);
            carrinhoCliente.set(idxCar, montarLinha(c));
        } else {
            carrinhoCliente.add(montarLinha(p[0], p[1], p[2], String.valueOf(qtd), p[4], p[3], p[6]));
        }
        System.out.println("[OK] Produto adicionado ao carrinho.");
    }

    private static void finalizarCompraCliente(Scanner sc) {
        if (carrinhoCliente.isEmpty()) {
            System.out.println("[AVISO] Carrinho vazio.");
            return;
        }
        System.out.print("CPF do cliente (somente numeros ou CANCELAR): ");
        String cpfRaw = sc.nextLine().trim();
        if ("CANCELAR".equalsIgnoreCase(cpfRaw) || cpfRaw.isEmpty()) {
            System.out.println("[INFO] Compra cancelada.");
            return;
        }
        String cpf = cpfRaw.replaceAll("\\D", "");
        if (cpf.length() != 11) {
            System.out.println("[AVISO] CPF invalido (deve conter 11 digitos). Compra cancelada.");
            return;
        }

        System.out.print("Nome completo do cliente (ou CANCELAR): ");
        String nome = sc.nextLine().trim();
        if ("CANCELAR".equalsIgnoreCase(nome) || nome.isEmpty()) {
            System.out.println("[INFO] Compra cancelada.");
            return;
        }

        int idxCli = encontrarIndicePorChave(clientes, 1, cpf);
        if (idxCli == -1) {
            String novoId = String.format("CLI-%03d", clientes.size() + 1);
            clientes.add(montarLinha(novoId, cpf, nome));
        } else {
            String[] cliExistente = partirLinha(clientes.get(idxCli));
            cliExistente[2] = nome;
            clientes.set(idxCli, montarLinha(cliExistente));
        }

        double subtotalGeral = 0.0;
        double impostoGeral = 0.0;
        double custoGeral = 0.0;
        StringBuilder resumoItens = new StringBuilder();

        for (String itemLinha : carrinhoCliente) {
            String[] c = partirLinha(itemLinha);
            String codPrd = c[0];
            int qtd = lerIntSafe(c[3], 0);
            int idxPrd = encontrarIndicePorChave(produtos, 0, codPrd);
            if (idxPrd == -1) {
                System.out.println("[AVISO] Produto " + codPrd + " nao encontrado. Compra cancelada.");
                return;
            }
            String[] p = partirLinha(produtos.get(idxPrd));
            int estAtual = lerIntSafe(p[5], 0);
            if (qtd > estAtual) {
                System.out.println("[AVISO] Estoque insuficiente para " + codPrd + " (" + estAtual + "). Compra cancelada.");
                return;
            }

            double custoUnit = lerDoubleSafe(p[3], 0.0);
            double precoUnit = lerDoubleSafe(p[4], 0.0);
            double aliq = lerDoubleSafe(p[6], aliquotaAnoAtual);

            double sub = Math.round(precoUnit * qtd * 100.0) / 100.0;
            double imp = Math.round(sub * (aliq / 100.0) * 100.0) / 100.0;
            double cst = Math.round(custoUnit * qtd * 100.0) / 100.0;

            subtotalGeral = Math.round((subtotalGeral + sub) * 100.0) / 100.0;
            impostoGeral = Math.round((impostoGeral + imp) * 100.0) / 100.0;
            custoGeral = Math.round((custoGeral + cst) * 100.0) / 100.0;

            p[5] = String.valueOf(estAtual - qtd);
            produtos.set(idxPrd, montarLinha(p));
            registrarMovEstoque("SAIDA", p[0], p[1], p[2], qtd, "Venda cliente " + cpf);

            if (resumoItens.length() > 0) resumoItens.append(",");
            resumoItens.append(codPrd).append("x").append(qtd);
        }

        double lucroLiq = Math.round((subtotalGeral - custoGeral - impostoGeral) * 100.0) / 100.0;
        double totalNf = Math.round((subtotalGeral + impostoGeral) * 100.0) / 100.0;
        String nfStr = "NF-" + (proxNf++);

        String linhaVenda = montarLinha(
                nfStr, dataHojeFormatada(), cpf, nome, resumoItens.toString(),
                String.format(Locale.US, "%.2f", subtotalGeral),
                String.format(Locale.US, "%.2f", impostoGeral),
                String.format(Locale.US, "%.2f", custoGeral),
                String.format(Locale.US, "%.2f", lucroLiq),
                String.format(Locale.US, "%.2f", totalNf)
        );
        histVendas.add(linhaVenda);
        registrarLancamentoFinanceiro("VENDA", "Venda " + nfStr + " - " + nome, totalNf);
        registrarAcesso("COMPRA_CLIENTE", nfStr + " (" + cpf + ")", "OK");
        carrinhoCliente.clear();
        salvarTodosArquivosTxtEBackup();

        System.out.println("\n=== NOTA FISCAL EMITIDA: " + nfStr + " ===");
        System.out.println("Cliente: " + nome + " | CPF: " + cpf);
        System.out.println("Itens: " + resumoItens);
        System.out.println("Subtotal: " + formatarMoeda(subtotalGeral));
        System.out.println("Impostos: " + formatarMoeda(impostoGeral));
        System.out.println("Total NF: " + formatarMoeda(totalNf));
    }

    // ==================== 2. MEU PERFIL ====================

    private static void exibirMeuPerfil() {
        int idx = encontrarIndicePorChave(funcionarios, 0, usuarioLogadoId);
        if (idx == -1) {
            System.out.println("[AVISO] Dados do perfil nao encontrados.");
            return;
        }
        String[] f = partirLinha(funcionarios.get(idx));
        int idade = calcularIdade(f[3]);
        System.out.println("\n=== MEU PERFIL ===");
        System.out.println("ID: " + f[0]);
        System.out.println("Nome: " + f[1]);
        System.out.println("CPF: " + f[2]);
        System.out.println("Data de Nascimento: " + f[3] + (idade >= 0 ? " (" + idade + " anos)" : ""));
        System.out.println("Sexo: " + f[4]);
        System.out.println("Cargo: " + f[5]);
        System.out.println("Salario: " + formatarMoeda(lerDoubleSafe(f[7], 0.0)));
        System.out.println("Pago: " + ("true".equalsIgnoreCase(f[8]) ? "SIM" : "NAO"));
        System.out.println("Ativo: " + ("true".equalsIgnoreCase(f[9]) ? "SIM" : "NAO"));
        System.out.println("Bloqueado: " + ("true".equalsIgnoreCase(f[10]) ? "SIM" : "NAO"));
        System.out.println("Autoridades: " + (f.length > 13 ? f[13] : ""));
        registrarAcesso("CONSULTA_PERFIL", usuarioLogadoId, "OK");
        salvarTodosArquivosTxtEBackup();
    }

    // ==================== 3. ESTOQUE E FORNECEDORES ====================

    private static void menuEstoqueEFornecedores(Scanner sc) {
        while (true) {
            System.out.println("\n=== ESTOQUE E FORNECEDORES ===");
            System.out.println("Saldo em caixa: " + formatarMoeda(saldoCaixa));
            System.out.println("1 - Listar produtos e informacoes");
            System.out.println("2 - Cadastrar produto (com fornecedor obrigatorio)");
            System.out.println("3 - Editar produto (modal)");
            System.out.println("4 - Registrar movimentacao de estoque (ENTRADA / SAIDA)");
            System.out.println("5 - Comprar produto de fornecedor (sincroniza estoque e caixa)");
            System.out.println("6 - Excluir produto (mantem historico de estoque)");
            System.out.println("7 - Listar fornecedores");
            System.out.println("8 - Cadastrar fornecedor (vinculado a produto)");
            System.out.println("9 - Editar fornecedor (modal)");
            System.out.println("10 - Consultar historico de movimentacoes de estoque");
            System.out.println("0 - Voltar");
            System.out.print("Escolha: ");

            String op = sc.nextLine().trim();
            switch (op) {
                case "1":
                    listarProdutosDetalhado();
                    break;
                case "2":
                    cadastrarProdutoComFornecedor(sc);
                    break;
                case "3":
                    editarProdutoModal(sc);
                    break;
                case "4":
                    registrarMovimentacaoManualEstoque(sc);
                    break;
                case "5":
                    comprarProdutoDeFornecedor(sc);
                    break;
                case "6":
                    excluirProdutoMantendoHistorico(sc);
                    break;
                case "7":
                    listarFornecedoresDetalhado();
                    break;
                case "8":
                    cadastrarFornecedorComProduto(sc);
                    break;
                case "9":
                    editarFornecedorModal(sc);
                    break;
                case "10":
                    exibirListaSimples("HISTORICO DE ESTOQUE", histEstoque);
                    break;
                case "0":
                    return;
                default:
                    System.out.println("[AVISO] Opcao invalida.");
                    break;
            }
        }
    }

    private static void listarProdutosDetalhado() {
        System.out.println("\n--- PRODUTOS CADASTRADOS ---");
        for (String linha : produtos) {
            String[] p = partirLinha(linha);
            double custo = lerDoubleSafe(p[3], 0.0);
            double preco = lerDoubleSafe(p[4], 0.0);
            System.out.println(p[0] + " | " + p[1] + " | Fornecedor: " + p[2]
                    + " | Custo: " + formatarMoeda(custo)
                    + " | Preco Venda: " + formatarMoeda(preco)
                    + " | Estoque: " + p[5]
                    + " | Aliq Atual/Prox: " + p[6] + "%/" + p[7] + "%"
                    + " | Ativo: " + p[8]);
        }
    }

    private static void cadastrarProdutoComFornecedor(Scanner sc) {
        if (fornecedores.isEmpty()) {
            System.out.println("[AVISO] Cadastre um fornecedor antes de cadastrar produto.");
            return;
        }
        String id = normalizarTextoObrigatorio(sc, "ID do produto (ex.: PRD-002)", true);
        if (id == null) return;
        if (encontrarIndicePorChave(produtos, 0, id) >= 0) {
            System.out.println("[AVISO] ID de produto ja existe.");
            return;
        }
        String nome = normalizarTextoObrigatorio(sc, "Nome do produto", true);
        if (nome == null) return;
        String forId = normalizarTextoObrigatorio(sc, "ID do fornecedor existente", true);
        if (forId == null) return;
        if (encontrarIndicePorChave(fornecedores, 0, forId) == -1) {
            System.out.println("[AVISO] Fornecedor nao encontrado. Todo produto exige fornecedor cadastrado.");
            return;
        }

        Double custo = lerDoublePositivo(sc, "Custo unitario do fornecedor", true);
        if (custo == null) return;
        Double preco = lerDoublePositivo(sc, "Preco de venda ao cliente (deve ser > custo)", true);
        if (preco == null) return;
        if (preco <= custo) {
            System.out.println("[AVISO] Preco de venda deve ser estritamente maior que o custo (" + formatarMoeda(custo) + ").");
            return;
        }
        Integer est = lerInteiroSeguro(sc, "Estoque inicial", 0, null, true);
        if (est == null) return;

        String novaLinha = montarLinha(
                id.toUpperCase(Locale.ROOT), nome, forId.toUpperCase(Locale.ROOT),
                String.format(Locale.US, "%.2f", custo),
                String.format(Locale.US, "%.2f", preco),
                String.valueOf(est),
                String.format(Locale.US, "%.2f", aliquotaAnoAtual),
                String.format(Locale.US, "%.2f", aliquotaProximoAno),
                "true"
        );
        produtos.add(novaLinha);
        if (est > 0) {
            registrarMovEstoque("ENTRADA", id.toUpperCase(Locale.ROOT), nome, forId.toUpperCase(Locale.ROOT), est, "Cadastro inicial produto");
        }
        registrarAcesso("CADASTRO_PRODUTO", id.toUpperCase(Locale.ROOT), "OK");
        salvarTodosArquivosTxtEBackup();
        System.out.println("[OK] Produto cadastrado com sucesso.");
    }

    private static void editarProdutoModal(Scanner sc) {
        String id = normalizarTextoObrigatorio(sc, "ID do produto para editar", true);
        if (id == null) return;
        int idx = encontrarIndicePorChave(produtos, 0, id);
        if (idx == -1) {
            System.out.println("[AVISO] Produto nao encontrado.");
            return;
        }
        String[] p = partirLinha(produtos.get(idx));
        while (true) {
            System.out.println("\n=== MODAL EDITAR PRODUTO (" + p[0] + ") ===");
            System.out.println("1 - Nome: " + p[1]);
            System.out.println("2 - Fornecedor ID: " + p[2]);
            System.out.println("3 - Custo unitario: " + p[3]);
            System.out.println("4 - Preco venda: " + p[4]);
            System.out.println("5 - Estoque: " + p[5]);
            System.out.println("6 - Status ativo: " + p[8]);
            System.out.println("0 - Salvar e fechar modal");
            System.out.print("Campo: ");
            String op = sc.nextLine().trim();
            if ("0".equals(op)) {
                produtos.set(idx, montarLinha(p));
                registrarAcesso("EDITAR_PRODUTO", p[0], "OK");
                salvarTodosArquivosTxtEBackup();
                System.out.println("[OK] Produto atualizado.");
                return;
            } else if ("1".equals(op)) {
                String v = normalizarTextoObrigatorio(sc, "Novo nome", true);
                if (v != null) p[1] = v;
            } else if ("2".equals(op)) {
                String v = normalizarTextoObrigatorio(sc, "Novo fornecedor ID", true);
                if (v != null && encontrarIndicePorChave(fornecedores, 0, v) >= 0) {
                    p[2] = v.toUpperCase(Locale.ROOT);
                } else {
                    System.out.println("[AVISO] Fornecedor inexistente.");
                }
            } else if ("3".equals(op)) {
                Double v = lerDoublePositivo(sc, "Novo custo unitario", true);
                if (v != null) {
                    double precoAtual = lerDoubleSafe(p[4], 0.0);
                    if (precoAtual <= v) {
                        System.out.println("[AVISO] Custo nao pode ser >= preco de venda (" + precoAtual + ").");
                    } else {
                        p[3] = String.format(Locale.US, "%.2f", v);
                    }
                }
            } else if ("4".equals(op)) {
                Double v = lerDoublePositivo(sc, "Novo preco de venda", true);
                if (v != null) {
                    double custoAtual = lerDoubleSafe(p[3], 0.0);
                    if (v <= custoAtual) {
                        System.out.println("[AVISO] Preco de venda deve ser > custo (" + custoAtual + ").");
                    } else {
                        p[4] = String.format(Locale.US, "%.2f", v);
                    }
                }
            } else if ("5".equals(op)) {
                Integer v = lerInteiroSeguro(sc, "Novo estoque", 0, null, true);
                if (v != null) p[5] = String.valueOf(v);
            } else if ("6".equals(op)) {
                p[8] = "true".equalsIgnoreCase(p[8]) ? "false" : "true";
                System.out.println("[INFO] Status ativo alterado para: " + p[8]);
            }
        }
    }

    private static void registrarMovimentacaoManualEstoque(Scanner sc) {
        String tipo = normalizarTextoObrigatorio(sc, "Tipo (ENTRADA ou SAIDA)", true);
        if (tipo == null) return;
        tipo = tipo.toUpperCase(Locale.ROOT);
        if (!"ENTRADA".equals(tipo) && !"SAIDA".equals(tipo)) {
            System.out.println("[AVISO] Informe ENTRADA ou SAIDA.");
            return;
        }
        String id = normalizarTextoObrigatorio(sc, "ID do produto", true);
        if (id == null) return;
        int idx = encontrarIndicePorChave(produtos, 0, id);
        if (idx == -1) {
            System.out.println("[AVISO] Produto nao encontrado.");
            return;
        }
        String[] p = partirLinha(produtos.get(idx));
        int estAtual = lerIntSafe(p[5], 0);
        Integer qtd = lerInteiroSeguro(sc, "Quantidade (Estoque atual: " + estAtual + ")", 1, null, true);
        if (qtd == null) return;
        if ("SAIDA".equals(tipo) && qtd > estAtual) {
            System.out.println("[AVISO] Estoque insuficiente para saida.");
            return;
        }
        String obs = normalizarTextoObrigatorio(sc, "Observacao da movimentacao", false);
        if (obs == null) obs = "Movimentacao manual";

        int novoEst = "ENTRADA".equals(tipo) ? estAtual + qtd : estAtual - qtd;
        p[5] = String.valueOf(novoEst);
        produtos.set(idx, montarLinha(p));
        registrarMovEstoque(tipo, p[0], p[1], p[2], qtd, obs);
        registrarAcesso("MOV_ESTOQUE_" + tipo, p[0] + " qtd=" + qtd, "OK");
        salvarTodosArquivosTxtEBackup();
        System.out.println("[OK] Movimentacao registrada. Novo estoque: " + novoEst);
    }

    private static void comprarProdutoDeFornecedor(Scanner sc) {
        String codPrd = normalizarTextoObrigatorio(sc, "ID do produto para compra", true);
        if (codPrd == null) return;
        int idxPrd = encontrarIndicePorChave(produtos, 0, codPrd);
        if (idxPrd == -1) {
            System.out.println("[AVISO] Produto nao encontrado.");
            return;
        }
        String[] p = partirLinha(produtos.get(idxPrd));
        String forId = p[2];
        if (encontrarIndicePorChave(fornecedores, 0, forId) == -1) {
            System.out.println("[AVISO] Fornecedor vinculado " + forId + " nao encontrado.");
            return;
        }
        double custoUnit = lerDoubleSafe(p[3], 0.0);
        Integer qtd = lerInteiroSeguro(sc, "Quantidade a comprar (Custo un.: " + formatarMoeda(custoUnit) + ")", 1, null, true);
        if (qtd == null) return;

        double totalCompra = Math.round(custoUnit * qtd * 100.0) / 100.0;
        if (saldoCaixa < totalCompra) {
            System.out.println("[AVISO] Saldo insuficiente em caixa (" + formatarMoeda(saldoCaixa) + ") para compra de " + formatarMoeda(totalCompra) + ".");
            return;
        }

        int novoEst = lerIntSafe(p[5], 0) + qtd;
        p[5] = String.valueOf(novoEst);
        p[8] = "true";
        produtos.set(idxPrd, montarLinha(p));

        registrarLancamentoFinanceiro("COMPRA_FORNECEDOR", "Compra " + p[0] + " x" + qtd + " (" + forId + ")", -totalCompra);
        registrarMovEstoque("ENTRADA", p[0], p[1], forId, qtd, "Compra de fornecedor " + forId);
        registrarAcesso("COMPRA_FORNECEDOR", p[0] + " x" + qtd + " valor=" + totalCompra, "OK");
        salvarTodosArquivosTxtEBackup();
        System.out.println("[OK] Compra concluida! Total pago: " + formatarMoeda(totalCompra) + " | Novo estoque: " + novoEst);
    }

    private static void excluirProdutoMantendoHistorico(Scanner sc) {
        String id = normalizarTextoObrigatorio(sc, "ID do produto para excluir", true);
        if (id == null) return;
        int idx = encontrarIndicePorChave(produtos, 0, id);
        if (idx == -1) {
            System.out.println("[AVISO] Produto nao encontrado.");
            return;
        }
        String[] p = partirLinha(produtos.get(idx));
        int est = lerIntSafe(p[5], 0);
        registrarMovEstoque("EXCLUSAO", p[0], p[1], p[2], est, "Produto removido do catalogo ativo (historico preservado)");
        produtos.remove(idx);
        registrarAcesso("EXCLUIR_PRODUTO", p[0], "OK");
        salvarTodosArquivosTxtEBackup();
        System.out.println("[OK] Produto excluido do catalogo ativo; historico de estoque preservado.");
    }

    private static void listarFornecedoresDetalhado() {
        System.out.println("\n--- FORNECEDORES CADASTRADOS ---");
        for (String linha : fornecedores) {
            String[] f = partirLinha(linha);
            System.out.println(f[0] + " | " + f[1] + " | CNPJ: " + f[2] + " | UF: " + f[3]
                    + " | Produto: " + f[4] + " | Valor Padrao: " + f[5]);
        }
    }

    private static void cadastrarFornecedorComProduto(Scanner sc) {
        String id = normalizarTextoObrigatorio(sc, "ID do fornecedor (ex.: FOR-002)", true);
        if (id == null) return;
        if (encontrarIndicePorChave(fornecedores, 0, id) >= 0) {
            System.out.println("[AVISO] ID de fornecedor ja existe.");
            return;
        }
        String nome = normalizarTextoObrigatorio(sc, "Nome/Razao social do fornecedor", true);
        if (nome == null) return;
        String cnpjRaw = normalizarTextoObrigatorio(sc, "CNPJ (14 digitos)", true);
        if (cnpjRaw == null) return;
        String cnpj = cnpjRaw.replaceAll("\\D", "");
        if (cnpj.length() != 14) {
            System.out.println("[AVISO] CNPJ invalido (deve conter 14 digitos).");
            return;
        }
        String uf = normalizarTextoObrigatorio(sc, "UF (ex.: SP)", true);
        if (uf == null) return;
        uf = uf.toUpperCase(Locale.ROOT);

        System.out.println("Todo fornecedor deve fornecer um produto:");
        System.out.println("1 - Vincular a um produto existente");
        System.out.println("2 - Cadastrar novo produto para este fornecedor");
        System.out.print("Escolha: ");
        String modo = sc.nextLine().trim();

        String prdIdVinculado = null;
        double valorPadrao = 0.0;

        if ("1".equals(modo)) {
            String codPrd = normalizarTextoObrigatorio(sc, "ID do produto existente", true);
            if (codPrd == null) return;
            int idxPrd = encontrarIndicePorChave(produtos, 0, codPrd);
            if (idxPrd == -1) {
                System.out.println("[AVISO] Produto nao encontrado.");
                return;
            }
            String[] p = partirLinha(produtos.get(idxPrd));
            prdIdVinculado = p[0];
            valorPadrao = lerDoubleSafe(p[3], 10.0);
            fornecedores.add(montarLinha(id.toUpperCase(Locale.ROOT), nome, cnpj, uf, prdIdVinculado,
                    String.format(Locale.US, "%.2f", valorPadrao)));
            p[2] = id.toUpperCase(Locale.ROOT);
            produtos.set(idxPrd, montarLinha(p));
        } else if ("2".equals(modo)) {
            String novoPrdId = normalizarTextoObrigatorio(sc, "ID do novo produto (ex.: PRD-002)", true);
            if (novoPrdId == null) return;
            if (encontrarIndicePorChave(produtos, 0, novoPrdId) >= 0) {
                System.out.println("[AVISO] ID de produto ja existe.");
                return;
            }
            String nomePrd = normalizarTextoObrigatorio(sc, "Nome do novo produto", true);
            if (nomePrd == null) return;
            Double custo = lerDoublePositivo(sc, "Custo unitario do produto", true);
            if (custo == null) return;
            Double preco = lerDoublePositivo(sc, "Preco de venda (deve ser > custo)", true);
            if (preco == null || preco <= custo) {
                System.out.println("[AVISO] Preco de venda deve ser maior que o custo.");
                return;
            }
            Integer est = lerInteiroSeguro(sc, "Estoque inicial", 0, null, true);
            if (est == null) return;

            prdIdVinculado = novoPrdId.toUpperCase(Locale.ROOT);
            valorPadrao = custo;
            fornecedores.add(montarLinha(id.toUpperCase(Locale.ROOT), nome, cnpj, uf, prdIdVinculado,
                    String.format(Locale.US, "%.2f", valorPadrao)));
            produtos.add(montarLinha(prdIdVinculado, nomePrd, id.toUpperCase(Locale.ROOT),
                    String.format(Locale.US, "%.2f", custo),
                    String.format(Locale.US, "%.2f", preco),
                    String.valueOf(est),
                    String.format(Locale.US, "%.2f", aliquotaAnoAtual),
                    String.format(Locale.US, "%.2f", aliquotaProximoAno),
                    "true"));
            if (est > 0) {
                registrarMovEstoque("ENTRADA", prdIdVinculado, nomePrd, id.toUpperCase(Locale.ROOT), est, "Cadastro com novo fornecedor");
            }
        } else {
            System.out.println("[AVISO] Opcao invalida.");
            return;
        }

        registrarAcesso("CADASTRO_FORNECEDOR", id.toUpperCase(Locale.ROOT) + " -> " + prdIdVinculado, "OK");
        salvarTodosArquivosTxtEBackup();
        System.out.println("[OK] Fornecedor cadastrado com vinculo de produto.");
    }

    private static void editarFornecedorModal(Scanner sc) {
        String id = normalizarTextoObrigatorio(sc, "ID do fornecedor para editar", true);
        if (id == null) return;
        int idx = encontrarIndicePorChave(fornecedores, 0, id);
        if (idx == -1) {
            System.out.println("[AVISO] Fornecedor nao encontrado.");
            return;
        }
        String[] f = partirLinha(fornecedores.get(idx));
        while (true) {
            System.out.println("\n=== MODAL EDITAR FORNECEDOR (" + f[0] + ") ===");
            System.out.println("1 - Nome: " + f[1]);
            System.out.println("2 - CNPJ: " + f[2]);
            System.out.println("3 - UF: " + f[3]);
            System.out.println("4 - Produto vinculado: " + f[4]);
            System.out.println("5 - Valor padrao: " + f[5]);
            System.out.println("0 - Salvar e fechar modal");
            System.out.print("Campo: ");
            String op = sc.nextLine().trim();
            if ("0".equals(op)) {
                fornecedores.set(idx, montarLinha(f));
                registrarAcesso("EDITAR_FORNECEDOR", f[0], "OK");
                salvarTodosArquivosTxtEBackup();
                System.out.println("[OK] Fornecedor atualizado.");
                return;
            } else if ("1".equals(op)) {
                String v = normalizarTextoObrigatorio(sc, "Novo nome", true);
                if (v != null) f[1] = v;
            } else if ("2".equals(op)) {
                String v = normalizarTextoObrigatorio(sc, "Novo CNPJ (14 digitos)", true);
                if (v != null && v.replaceAll("\\D", "").length() == 14) {
                    f[2] = v.replaceAll("\\D", "");
                } else {
                    System.out.println("[AVISO] CNPJ invalido.");
                }
            } else if ("3".equals(op)) {
                String v = normalizarTextoObrigatorio(sc, "Nova UF", true);
                if (v != null) f[3] = v.toUpperCase(Locale.ROOT);
            } else if ("4".equals(op)) {
                String v = normalizarTextoObrigatorio(sc, "ID do produto vinculado", true);
                if (v != null && encontrarIndicePorChave(produtos, 0, v) >= 0) {
                    f[4] = v.toUpperCase(Locale.ROOT);
                } else {
                    System.out.println("[AVISO] Produto nao encontrado.");
                }
            } else if ("5".equals(op)) {
                Double v = lerDoublePositivo(sc, "Novo valor padrao", true);
                if (v != null) f[5] = String.format(Locale.US, "%.2f", v);
            }
        }
    }

    // ==================== 4. RH ====================

    private static void menuRh(Scanner sc) {
        while (true) {
            System.out.println("\n=== MODULO RH ===");
            System.out.println("1 - Listar funcionarios");
            System.out.println("2 - Contratar / cadastrar novo funcionario");
            System.out.println("3 - Demitir funcionario (motivo obrigatorio)");
            System.out.println("4 - Readmitir funcionario");
            System.out.println("5 - Editar informacoes do funcionario (modal)");
            System.out.println("0 - Voltar");
            System.out.print("Escolha: ");

            String op = sc.nextLine().trim();
            switch (op) {
                case "1":
                    listarFuncionariosDetalhado();
                    break;
                case "2":
                    cadastrarFuncionarioRh(sc);
                    break;
                case "3":
                    demitirFuncionarioRh(sc);
                    break;
                case "4":
                    readmitirFuncionarioRh(sc);
                    break;
                case "5":
                    editarFuncionarioModalRh(sc);
                    break;
                case "0":
                    return;
                default:
                    System.out.println("[AVISO] Opcao invalida.");
                    break;
            }
        }
    }

    private static void listarFuncionariosDetalhado() {
        System.out.println("\n--- FUNCIONARIOS ---");
        for (String linha : funcionarios) {
            String[] f = partirLinha(linha);
            int idade = calcularIdade(f[3]);
            System.out.println(f[0] + " | " + f[1] + " | CPF: " + f[2]
                    + " | Nasc: " + f[3] + (idade >= 0 ? " (" + idade + "a)" : "")
                    + " | Sexo: " + f[4] + " | Cargo: " + f[5]
                    + " | Sal: " + formatarMoeda(lerDoubleSafe(f[7], 0.0))
                    + " | Pago: " + f[8] + " | Ativo: " + f[9] + " | Bloq: " + f[10]);
        }
    }

    private static void cadastrarFuncionarioRh(Scanner sc) {
        String id = normalizarTextoObrigatorio(sc, "ID do funcionario (ex.: UR-FUNC07)", true);
        if (id == null) return;
        if (encontrarIndicePorChave(funcionarios, 0, id) >= 0) {
            System.out.println("[AVISO] ID ja cadastrado.");
            return;
        }
        String nome = normalizarTextoObrigatorio(sc, "Nome completo", true);
        if (nome == null) return;
        String cpfRaw = normalizarTextoObrigatorio(sc, "CPF (11 digitos)", true);
        if (cpfRaw == null) return;
        String cpf = cpfRaw.replaceAll("\\D", "");
        if (cpf.length() != 11) {
            System.out.println("[AVISO] CPF invalido.");
            return;
        }
        String nasc = normalizarTextoObrigatorio(sc, "Data de nascimento (dd/MM/yyyy)", true);
        if (nasc == null || calcularIdade(nasc) < 0) {
            System.out.println("[AVISO] Data de nascimento invalida.");
            return;
        }
        String sexo = normalizarTextoObrigatorio(sc, "Sexo (M/F/O)", true);
        if (sexo == null) return;
        String cargo = normalizarTextoObrigatorio(sc, "Cargo (ESTOQUE, RH, CAIXA, FINANCEIRO, TI, GERENCIA)", true);
        if (cargo == null) return;
        cargo = cargo.toUpperCase(Locale.ROOT);
        if ("GERENCIA".equals(cargo) && !"GERENCIA".equalsIgnoreCase(usuarioLogadoCargo)) {
            System.out.println("[AVISO] Apenas GERENCIA pode cadastrar funcionario de GERENCIA.");
            return;
        }
        String senha = normalizarTextoObrigatorio(sc, "Senha inicial", true);
        if (senha == null) return;
        Double salario = lerDoublePositivo(sc, "Salario inicial", true);
        if (salario == null) return;

        String aut = autoridadesPadraoPorCargo(cargo);
        funcionarios.add(montarLinha(
                id.toUpperCase(Locale.ROOT), nome, cpf, nasc, sexo.toUpperCase(Locale.ROOT),
                cargo, senha, String.format(Locale.US, "%.2f", salario),
                "false", "true", "false", "0", "N/A", aut
        ));
        registrarAcesso("RH_CONTRATAR", id.toUpperCase(Locale.ROOT) + " (" + cargo + ")", "OK");
        salvarTodosArquivosTxtEBackup();
        System.out.println("[OK] Funcionario cadastrado com sucesso.");
    }

    private static void demitirFuncionarioRh(Scanner sc) {
        String ident = normalizarTextoObrigatorio(sc, "ID ou CPF do funcionario para demitir", true);
        if (ident == null) return;
        int idx = encontrarIndiceFuncionario(ident);
        if (idx == -1) {
            System.out.println("[AVISO] Funcionario nao encontrado.");
            return;
        }
        String[] f = partirLinha(funcionarios.get(idx));
        if ("GERENCIA".equalsIgnoreCase(f[5]) && !"GERENCIA".equalsIgnoreCase(usuarioLogadoCargo)) {
            System.out.println("[AVISO] Sem autoridade para demitir funcionario da GERENCIA.");
            return;
        }
        String motivo = normalizarTextoObrigatorio(sc, "Motivo da demissao (OBRIGATORIO)", true);
        if (motivo == null) {
            System.out.println("[AVISO] Demissao cancelada: motivo e obrigatorio.");
            return;
        }
        f[9] = "false";
        f[12] = motivo;
        funcionarios.set(idx, montarLinha(f));
        registrarAcesso("RH_DEMITIR", f[0] + " - Motivo: " + motivo, "OK");
        salvarTodosArquivosTxtEBackup();
        System.out.println("[OK] Funcionario " + f[0] + " demitido.");
    }

    private static void readmitirFuncionarioRh(Scanner sc) {
        String ident = normalizarTextoObrigatorio(sc, "ID ou CPF do funcionario para readmitir", true);
        if (ident == null) return;
        int idx = encontrarIndiceFuncionario(ident);
        if (idx == -1) {
            System.out.println("[AVISO] Funcionario nao encontrado.");
            return;
        }
        String[] f = partirLinha(funcionarios.get(idx));
        f[9] = "true";
        f[10] = "false";
        f[11] = "0";
        f[12] = "READMITIDO";
        funcionarios.set(idx, montarLinha(f));
        registrarAcesso("RH_READMITIR", f[0], "OK");
        salvarTodosArquivosTxtEBackup();
        System.out.println("[OK] Funcionario " + f[0] + " readmitido.");
    }

    private static void editarFuncionarioModalRh(Scanner sc) {
        String ident = normalizarTextoObrigatorio(sc, "ID ou CPF do funcionario para editar", true);
        if (ident == null) return;
        int idx = encontrarIndiceFuncionario(ident);
        if (idx == -1) {
            System.out.println("[AVISO] Funcionario nao encontrado.");
            return;
        }
        String[] f = partirLinha(funcionarios.get(idx));
        if ("GERENCIA".equalsIgnoreCase(f[5]) && !"GERENCIA".equalsIgnoreCase(usuarioLogadoCargo)) {
            System.out.println("[AVISO] Alterar informacoes da GERENCIA exige cargo GERENCIA.");
            return;
        }
        while (true) {
            System.out.println("\n=== MODAL EDITAR FUNCIONARIO (" + f[0] + ") ===");
            System.out.println("1 - Nome: " + f[1]);
            System.out.println("2 - Data Nascimento: " + f[3] + " (Idade: " + calcularIdade(f[3]) + ")");
            System.out.println("3 - Sexo: " + f[4]);
            System.out.println("4 - Cargo: " + f[5]);
            System.out.println("5 - Salario: " + f[7]);
            System.out.println("6 - Status pago: " + f[8]);
            System.out.println("0 - Salvar e fechar modal");
            System.out.print("Campo: ");
            String op = sc.nextLine().trim();
            if ("0".equals(op)) {
                funcionarios.set(idx, montarLinha(f));
                registrarAcesso("RH_EDITAR_FUNCIONARIO", f[0], "OK");
                salvarTodosArquivosTxtEBackup();
                System.out.println("[OK] Funcionario atualizado.");
                return;
            } else if ("1".equals(op)) {
                String v = normalizarTextoObrigatorio(sc, "Novo nome", true);
                if (v != null) f[1] = v;
            } else if ("2".equals(op)) {
                String v = normalizarTextoObrigatorio(sc, "Nova data nascimento (dd/MM/yyyy)", true);
                if (v != null && calcularIdade(v) >= 0) f[3] = v;
                else System.out.println("[AVISO] Data invalida.");
            } else if ("3".equals(op)) {
                String v = normalizarTextoObrigatorio(sc, "Novo sexo (M/F/O)", true);
                if (v != null) f[4] = v.toUpperCase(Locale.ROOT);
            } else if ("4".equals(op)) {
                String v = normalizarTextoObrigatorio(sc, "Novo cargo", true);
                if (v != null) {
                    f[5] = v.toUpperCase(Locale.ROOT);
                    f[13] = autoridadesPadraoPorCargo(f[5]);
                }
            } else if ("5".equals(op)) {
                Double v = lerDoublePositivo(sc, "Novo salario", true);
                if (v != null) f[7] = String.format(Locale.US, "%.2f", v);
            } else if ("6".equals(op)) {
                f[8] = "true".equalsIgnoreCase(f[8]) ? "false" : "true";
            }
        }
    }

    // ==================== 5. CAIXA ====================

    private static void menuCaixa(Scanner sc) {
        while (true) {
            System.out.println("\n=== MODULO CAIXA ===");
            System.out.println("Saldo atual: " + formatarMoeda(saldoCaixa));
            System.out.println("1 - Registrar venda e emitir Nota Fiscal");
            System.out.println("2 - Consultar historico de vendas / Notas Fiscais");
            System.out.println("0 - Voltar");
            System.out.print("Escolha: ");
            String op = sc.nextLine().trim();
            if ("1".equals(op)) {
                registrarVendaCaixa(sc);
            } else if ("2".equals(op)) {
                exibirListaSimples("HISTORICO DE VENDAS", histVendas);
            } else if ("0".equals(op)) {
                return;
            } else {
                System.out.println("[AVISO] Opcao invalida.");
            }
        }
    }

    private static void registrarVendaCaixa(Scanner sc) {
        if (produtos.isEmpty()) {
            System.out.println("[AVISO] Nenhum produto cadastrado.");
            return;
        }
        String codPrd = normalizarTextoObrigatorio(sc, "Codigo do produto (ex.: PRD-001)", true);
        if (codPrd == null) return;
        int idxPrd = encontrarIndicePorChave(produtos, 0, codPrd);
        if (idxPrd == -1) {
            System.out.println("[AVISO] Produto nao encontrado.");
            return;
        }
        String[] p = partirLinha(produtos.get(idxPrd));
        int estoqueAtual = lerIntSafe(p[5], 0);
        Integer qtd = lerInteiroSeguro(sc, "Quantidade (Estoque: " + estoqueAtual + ")", 1, estoqueAtual, true);
        if (qtd == null) return;

        System.out.print("CPF do cliente (ou vazio para cliente padrao): ");
        String cpfCli = sc.nextLine().trim().replaceAll("\\D", "");
        String nomeCli = "CONSUMIDOR FINAL";
        if (!cpfCli.isEmpty()) {
            int idxCli = encontrarIndicePorChave(clientes, 1, cpfCli);
            if (idxCli >= 0) {
                nomeCli = partirLinha(clientes.get(idxCli))[2];
            } else {
                System.out.print("Nome do cliente: ");
                String n = sc.nextLine().trim();
                if (!n.isEmpty()) nomeCli = n;
                clientes.add(montarLinha(String.format("CLI-%03d", clientes.size() + 1), cpfCli, nomeCli));
            }
        } else {
            cpfCli = "00000000000";
        }

        double custoUnit = lerDoubleSafe(p[3], 0.0);
        double precoUnit = lerDoubleSafe(p[4], 0.0);
        double aliq = lerDoubleSafe(p[6], aliquotaAnoAtual);

        int novoEstoque = estoqueAtual - qtd;
        p[5] = String.valueOf(novoEstoque);
        produtos.set(idxPrd, montarLinha(p));

        double subtotal = Math.round(precoUnit * qtd * 100.0) / 100.0;
        double imposto = Math.round(subtotal * (aliq / 100.0) * 100.0) / 100.0;
        double custoTotal = Math.round(custoUnit * qtd * 100.0) / 100.0;
        double lucroLiq = Math.round((subtotal - custoTotal - imposto) * 100.0) / 100.0;
        double totalNf = Math.round((subtotal + imposto) * 100.0) / 100.0;

        String nfStr = "NF-" + (proxNf++);
        histVendas.add(montarLinha(
                nfStr, dataHojeFormatada(), cpfCli, nomeCli, p[0] + "x" + qtd,
                String.format(Locale.US, "%.2f", subtotal),
                String.format(Locale.US, "%.2f", imposto),
                String.format(Locale.US, "%.2f", custoTotal),
                String.format(Locale.US, "%.2f", lucroLiq),
                String.format(Locale.US, "%.2f", totalNf)
        ));

        registrarMovEstoque("SAIDA", p[0], p[1], p[2], qtd, "Venda no caixa " + nfStr);
        registrarLancamentoFinanceiro("VENDA", "Venda " + nfStr, totalNf);
        registrarAcesso("VENDA_CAIXA", nfStr + " (" + p[0] + "x" + qtd + ")", "OK");
        salvarTodosArquivosTxtEBackup();

        System.out.println("\n=== NOTA FISCAL EMITIDA: " + nfStr + " ===");
        System.out.println("Cliente: " + nomeCli + " | CPF: " + cpfCli);
        System.out.println("Item: " + p[0] + " - " + p[1] + " x" + qtd);
        System.out.println("Subtotal: " + formatarMoeda(subtotal));
        System.out.println("Imposto (" + aliq + "%): " + formatarMoeda(imposto));
        System.out.println("Lucro liquido estimado: " + formatarMoeda(lucroLiq));
        System.out.println("Total NF: " + formatarMoeda(totalNf));
    }

    // ==================== 6. FINANCEIRO ====================

    private static void menuFinanceiro(Scanner sc) {
        while (true) {
            System.out.println("\n=== MODULO FINANCEIRO ===");
            System.out.println("Saldo atual: " + formatarMoeda(saldoCaixa)
                    + " | Aliq Atual: " + aliquotaAnoAtual + "% | Aliq Prox Ano: " + aliquotaProximoAno + "%");
            System.out.println("1 - Pagar salario de funcionario");
            System.out.println("2 - Ajustar salario de funcionario");
            System.out.println("3 - Pagar fornecedor / compra de estoque");
            System.out.println("4 - Definir aliquotas fiscais (ano atual e proximo ano)");
            System.out.println("5 - Consultar historico financeiro e resumo de lucro/impostos");
            System.out.println("0 - Voltar");
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
                    comprarProdutoDeFornecedor(sc);
                    break;
                case "4":
                    definirAliquotasFiscais(sc);
                    break;
                case "5":
                    exibirResumoFinanceiroEHistorico();
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
        String ident = normalizarTextoObrigatorio(sc, "ID ou CPF do funcionario", true);
        if (ident == null) return;
        int idx = encontrarIndiceFuncionario(ident);
        if (idx == -1) {
            System.out.println("[AVISO] Funcionario nao encontrado.");
            return;
        }
        String[] f = partirLinha(funcionarios.get(idx));
        if (!"true".equalsIgnoreCase(f[9])) {
            System.out.println("[AVISO] Funcionario inativo/demitido.");
            return;
        }
        if ("true".equalsIgnoreCase(f[8])) {
            System.out.println("[AVISO] Funcionario " + f[0] + " ja consta como PAGO.");
            return;
        }
        double sal = lerDoubleSafe(f[7], 0.0);
        if (saldoCaixa < sal) {
            System.out.println("[AVISO] Saldo insuficiente em caixa para pagar " + formatarMoeda(sal) + ".");
            return;
        }
        f[8] = "true";
        funcionarios.set(idx, montarLinha(f));
        registrarLancamentoFinanceiro("SALARIO", "Pagamento salario " + f[0] + " (" + f[1] + ")", -sal);
        registrarAcesso("PAGAR_SALARIO", f[0] + " valor=" + sal, "OK");
        salvarTodosArquivosTxtEBackup();
        System.out.println("[OK] Salario de " + formatarMoeda(sal) + " pago para " + f[1] + ".");
    }

    private static void ajustarSalarioFuncionario(Scanner sc) {
        String ident = normalizarTextoObrigatorio(sc, "ID ou CPF do funcionario", true);
        if (ident == null) return;
        int idx = encontrarIndiceFuncionario(ident);
        if (idx == -1) {
            System.out.println("[AVISO] Funcionario nao encontrado.");
            return;
        }
        String[] f = partirLinha(funcionarios.get(idx));
        Double novoSal = lerDoublePositivo(sc, "Novo salario (Atual: " + f[7] + ")", true);
        if (novoSal == null) return;
        f[7] = String.format(Locale.US, "%.2f", novoSal);
        f[8] = "false";
        funcionarios.set(idx, montarLinha(f));
        registrarAcesso("AJUSTAR_SALARIO", f[0] + " novo=" + novoSal, "OK");
        salvarTodosArquivosTxtEBackup();
        System.out.println("[OK] Salario atualizado para " + formatarMoeda(novoSal) + ".");
    }

    private static void definirAliquotasFiscais(Scanner sc) {
        Double atual = lerDoublePositivo(sc, "Aliquota deste ano (%) [Atual: " + aliquotaAnoAtual + "]", true);
        if (atual == null) return;
        Double prox = lerDoublePositivo(sc, "Aliquota do proximo ano (%) [Atual: " + aliquotaProximoAno + "]", true);
        if (prox == null) return;

        aliquotaAnoAtual = Math.round(atual * 100.0) / 100.0;
        aliquotaProximoAno = Math.round(prox * 100.0) / 100.0;

        for (int i = 0; i < produtos.size(); i++) {
            String[] p = partirLinha(produtos.get(i));
            if (p.length >= 8) {
                p[6] = String.format(Locale.US, "%.2f", aliquotaAnoAtual);
                p[7] = String.format(Locale.US, "%.2f", aliquotaProximoAno);
                produtos.set(i, montarLinha(p));
            }
        }
        registrarAcesso("DEFINIR_ALIQUOTAS", "ATUAL=" + aliquotaAnoAtual + "% PROX=" + aliquotaProximoAno + "%", "OK");
        salvarTodosArquivosTxtEBackup();
        System.out.println("[OK] Aliquotas atualizadas nos produtos e no controle.");
    }

    private static void exibirResumoFinanceiroEHistorico() {
        double totalImpostos = 0.0;
        double totalLucro = 0.0;
        for (String v : histVendas) {
            String[] p = partirLinha(v);
            totalImpostos += lerDoubleSafe(p[6], 0.0);
            totalLucro += lerDoubleSafe(p[8], 0.0);
        }
        System.out.println("\n=== RESUMO FINANCEIRO ===");
        System.out.println("Saldo atual: " + formatarMoeda(saldoCaixa));
        System.out.println("Total impostos nas vendas: " + formatarMoeda(totalImpostos));
        System.out.println("Total lucro liquido nas vendas: " + formatarMoeda(totalLucro));
        exibirListaSimples("HISTORICO FINANCEIRO", histFinanceiro);
    }

    // ==================== 7. TI ====================

    private static void menuTi(Scanner sc) {
        while (true) {
            System.out.println("\n=== MODULO TI ===");
            System.out.println("1 - Desbloquear login bloqueado (3 tentativas)");
            System.out.println("2 - Ajustar autoridades de funcionario");
            System.out.println("3 - Consultar historico de acessos e utilizacao");
            System.out.println("4 - Criar backup manual (.TXT) e sincronizar");
            System.out.println("5 - Consultar arquivos .TXT salvos");
            System.out.println("0 - Voltar");
            System.out.print("Escolha: ");

            String op = sc.nextLine().trim();
            switch (op) {
                case "1":
                    desbloquearLoginTi(sc);
                    break;
                case "2":
                    ajustarAutoridadesTi(sc);
                    break;
                case "3":
                    exibirListaSimples("HISTORICO DE ACESSOS", histAcessos);
                    break;
                case "4":
                    salvarTodosArquivosTxtEBackup();
                    registrarAcesso("TI_BACKUP_MANUAL", "SINCRONIZACAO_COMPLETA", "OK");
                    salvarTodosArquivosTxtEBackup();
                    System.out.println("[OK] Backup e .TXT atualizados.");
                    break;
                case "5":
                    consultarArquivoTxtSalvo(sc);
                    break;
                case "0":
                    return;
                default:
                    System.out.println("[AVISO] Opcao invalida.");
                    break;
            }
        }
    }

    private static void desbloquearLoginTi(Scanner sc) {
        String ident = normalizarTextoObrigatorio(sc, "ID ou CPF do funcionario bloqueado", true);
        if (ident == null) return;
        int idx = encontrarIndiceFuncionario(ident);
        if (idx == -1) {
            System.out.println("[AVISO] Funcionario nao encontrado.");
            return;
        }
        String[] f = partirLinha(funcionarios.get(idx));
        f[10] = "false";
        f[11] = "0";
        funcionarios.set(idx, montarLinha(f));
        registrarAcesso("TI_DESBLOQUEAR_LOGIN", f[0], "OK");
        salvarTodosArquivosTxtEBackup();
        System.out.println("[OK] Login desbloqueado para " + f[0] + ".");
    }

    private static void ajustarAutoridadesTi(Scanner sc) {
        String ident = normalizarTextoObrigatorio(sc, "ID ou CPF do funcionario", true);
        if (ident == null) return;
        int idx = encontrarIndiceFuncionario(ident);
        if (idx == -1) {
            System.out.println("[AVISO] Funcionario nao encontrado.");
            return;
        }
        String[] f = partirLinha(funcionarios.get(idx));
        if ("GERENCIA".equalsIgnoreCase(f[5])) {
            System.out.print("Para alterar informacoes da GERENCIA, informe a senha de confirmacao do gerente: ");
            String senhaConf = sc.nextLine().trim();
            if (!f[6].equals(senhaConf)) {
                System.out.println("[AVISO] Senha de confirmacao incorreta. Alteracao bloqueada.");
                registrarAcesso("TI_ALTERAR_GERENCIA_NEGADO", f[0], "NEGADO");
                return;
            }
        }
        String novasAut = normalizarTextoObrigatorio(sc, "Novas autoridades separadas por virgula (ex.: PERFIL,ESTOQUE,CAIXA)", true);
        if (novasAut == null) return;
        f[13] = novasAut.toUpperCase(Locale.ROOT);
        funcionarios.set(idx, montarLinha(f));
        registrarAcesso("TI_AJUSTAR_AUTORIDADE", f[0] + " -> " + f[13], "OK");
        salvarTodosArquivosTxtEBackup();
        System.out.println("[OK] Autoridades atualizadas para " + f[0] + ".");
    }

    private static void consultarArquivoTxtSalvo(Scanner sc) {
        System.out.println("Arquivos disponiveis: " + ARQ_BACKUP + ", " + ARQ_FUNCIONARIOS + ", "
                + ARQ_CLIENTES + ", " + ARQ_FORNECEDORES + ", " + ARQ_PRODUTOS + ", "
                + ARQ_FINANCEIRO + ", " + ARQ_VENDAS + ", " + ARQ_HIST_ESTOQUE + ", " + ARQ_HIST_ACESSOS);
        String nomeArq = normalizarTextoObrigatorio(sc, "Nome do arquivo .TXT para consultar", true);
        if (nomeArq == null) return;
        Path p = Paths.get(nomeArq);
        if (!Files.exists(p)) {
            System.out.println("[AVISO] Arquivo nao encontrado.");
            return;
        }
        List<String> linhas = lerLinhasSeguras(p);
        System.out.println("\n=== CONTEUDO DE " + nomeArq + " (" + linhas.size() + " linhas) ===");
        for (int i = 0; i < Math.min(linhas.size(), 50); i++) {
            System.out.println(linhas.get(i));
        }
        if (linhas.size() > 50) {
            System.out.println("... (" + (linhas.size() - 50) + " linhas adicionais)");
        }
    }

    // ==================== 8. GERENCIA ====================

    private static void menuGerencia(Scanner sc) {
        while (true) {
            System.out.println("\n=== MODULO GERENCIA (VISAO COMPLETA) ===");
            System.out.println("1 - Resumo geral do sistema");
            System.out.println("2 - Listar funcionarios");
            System.out.println("3 - Listar clientes");
            System.out.println("4 - Listar produtos e fornecedores");
            System.out.println("5 - Consultar historico de vendas e financeiro");
            System.out.println("6 - Consultar historico de estoque e acessos");
            System.out.println("0 - Voltar");
            System.out.print("Escolha: ");
            String op = sc.nextLine().trim();
            switch (op) {
                case "1":
                    imprimirResumo();
                    break;
                case "2":
                    listarFuncionariosDetalhado();
                    break;
                case "3":
                    exibirListaSimples("CLIENTES", clientes);
                    break;
                case "4":
                    listarProdutosDetalhado();
                    listarFornecedoresDetalhado();
                    break;
                case "5":
                    exibirListaSimples("VENDAS", histVendas);
                    exibirResumoFinanceiroEHistorico();
                    break;
                case "6":
                    exibirListaSimples("ESTOQUE", histEstoque);
                    exibirListaSimples("ACESSOS", histAcessos);
                    break;
                case "0":
                    return;
                default:
                    System.out.println("[AVISO] Opcao invalida.");
                    break;
            }
        }
    }

    // ==================== AUXILIARES E FORMATACAO ====================

    private static List<String> obterProdutosAtivosFiltrados(String filtro) {
        String f = filtro == null ? "" : filtro.trim().toLowerCase(Locale.ROOT);
        List<String> resultado = new ArrayList<>();
        for (String linha : produtos) {
            String[] p = partirLinha(linha);
            boolean ativo = p.length <= 8 || "true".equalsIgnoreCase(p[8]);
            if (!ativo) continue;
            if (f.isEmpty() || p[0].toLowerCase(Locale.ROOT).contains(f) || p[1].toLowerCase(Locale.ROOT).contains(f)) {
                resultado.add(linha);
            }
        }
        return resultado;
    }

    private static int encontrarIndiceFuncionario(String ident) {
        String limpo = ident.trim();
        String digitos = limpo.replaceAll("\\D", "");
        for (int i = 0; i < funcionarios.size(); i++) {
            String[] f = partirLinha(funcionarios.get(i));
            if (f[0].equalsIgnoreCase(limpo)) return i;
            if (!digitos.isEmpty() && f.length > 2 && f[2].replaceAll("\\D", "").equals(digitos)) return i;
        }
        return -1;
    }

    private static int calcularIdade(String dataNascStr) {
        try {
            LocalDate nasc = LocalDate.parse(dataNascStr.trim(), FMT_DATA);
            LocalDate hoje = LocalDate.now();
            if (nasc.isAfter(hoje)) return -1;
            return Period.between(nasc, hoje).getYears();
        } catch (DateTimeParseException e) {
            return -1;
        }
    }

    private static String dataHojeFormatada() {
        return LocalDate.now().format(FMT_DATA);
    }

    private static String dataHoraAgoraFormatada() {
        return LocalDateTime.now().format(FMT_DATA_HORA);
    }

    private static String normalizarTextoObrigatorio(Scanner sc, String rotulo, boolean permiteCancelar) {
        System.out.print(rotulo + ": ");
        String v = sc.nextLine().trim();
        if (v.isEmpty() || (permiteCancelar && "CANCELAR".equalsIgnoreCase(v))) {
            return null;
        }
        return v;
    }

    private static Integer lerInteiroSeguro(Scanner sc, String rotulo, int min, Integer max, boolean permiteCancelar) {
        System.out.print(rotulo + (permiteCancelar ? " (ou CANCELAR)" : "") + ": ");
        String entrada = sc.nextLine().trim();
        if (permiteCancelar && "CANCELAR".equalsIgnoreCase(entrada)) {
            return null;
        }
        try {
            int val = Integer.parseInt(entrada);
            if (val < min || (max != null && val > max)) {
                System.out.println("[AVISO] Valor fora do intervalo permitido.");
                return null;
            }
            return val;
        } catch (NumberFormatException e) {
            System.out.println("[AVISO] Numero inteiro invalido.");
            return null;
        }
    }

    private static Double lerDoublePositivo(Scanner sc, String rotulo, boolean permiteCancelar) {
        System.out.print(rotulo + (permiteCancelar ? " (ou CANCELAR)" : "") + ": ");
        String entrada = sc.nextLine().trim().replace(",", ".");
        if (permiteCancelar && "CANCELAR".equalsIgnoreCase(entrada)) {
            return null;
        }
        try {
            double val = Double.parseDouble(entrada);
            if (val <= 0) {
                System.out.println("[AVISO] Valor deve ser maior que zero.");
                return null;
            }
            return val;
        } catch (NumberFormatException e) {
            System.out.println("[AVISO] Valor numerico invalido.");
            return null;
        }
    }

    private static int lerIntSafe(String s, int padrao) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return padrao;
        }
    }

    private static double lerDoubleSafe(String s, double padrao) {
        try {
            return Double.parseDouble(s.trim().replace(",", "."));
        } catch (Exception e) {
            return padrao;
        }
    }

    private static int encontrarIndicePorChave(List<String> lista, int indiceCampo, String chave) {
        for (int i = 0; i < lista.size(); i++) {
            String[] p = partirLinha(lista.get(i));
            if (p.length > indiceCampo && p[indiceCampo].equalsIgnoreCase(chave)) {
                return i;
            }
        }
        return -1;
    }

    private static String[] partirLinha(String linha) {
        return linha.split(";", -1);
    }

    private static String montarLinha(String... campos) {
        return String.join(";", campos);
    }

    private static String formatarMoeda(double valor) {
        return "R$ " + String.format(Locale.US, "%.2f", valor);
    }

    private static void exibirListaSimples(String titulo, List<String> itens) {
        System.out.println("\n--- " + titulo + " (" + itens.size() + ") ---");
        for (String item : itens) {
            System.out.println(item);
        }
    }

    private static void imprimirResumo() {
        System.out.println("Funcionarios: " + funcionarios.size());
        System.out.println("Clientes: " + clientes.size());
        System.out.println("Fornecedores: " + fornecedores.size());
        System.out.println("Produtos: " + produtos.size());
        System.out.println("Historico Financeiro: " + histFinanceiro.size());
        System.out.println("Historico Vendas: " + histVendas.size());
        System.out.println("Historico Estoque: " + histEstoque.size());
        System.out.println("Historico Acessos: " + histAcessos.size());
    }
}
