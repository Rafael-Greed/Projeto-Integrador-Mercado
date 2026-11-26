import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class SistemaGestaoUnificado {

    // ==================== ARQUIVOS DE PERSISTENCIA E LOGS ====================
    private static final String ARQUIVO_DADOS = "dados_sistema.txt";
    private static final String ARQUIVO_BACKUP = "backup_sistema_editavel.txt";
    private static final String ARQUIVO_LOG_IO = "log_entradas_saidas_dados.txt";
    private static final String ARQUIVO_AUDITORIA = "auditoria_sistema.log";

    private static boolean encerramentoExecutado = false;

    // ==================== EXCECAO DE RETORNO / VOLTAR ====================
    static class RetornarTelaException extends RuntimeException {
        RetornarTelaException(String msg) {
            super(msg);
        }
    }

    // ==================== ENTIDADES ====================
    static class Funcionario {
        String ur;
        String nome;
        String cpf;
        String cargo; // GESTAO, RH, FINANCEIRO, ESTOQUE, CAIXA
        String senha;
        double salario;
        boolean bloqueado;
        String statusRH; // ATIVO, DEMITIDO
        int tentativasFalhas;

        Funcionario(String ur, String nome, String cpf, String cargo, String senha,
                    double salario, boolean bloqueado, String statusRH) {
            this.ur = ur;
            this.nome = nome;
            this.cpf = cpf;
            this.cargo = cargo;
            this.senha = senha;
            this.salario = salario;
            this.bloqueado = bloqueado;
            this.statusRH = statusRH;
            this.tentativasFalhas = 0;
        }
    }

    static class Cliente {
        String ur;
        String nome;
        String cpf;
        String senha;
        boolean bloqueado;
        int tentativasFalhas;

        Cliente(String ur, String nome, String cpf, String senha, boolean bloqueado) {
            this.ur = ur;
            this.nome = nome;
            this.cpf = cpf;
            this.senha = senha;
            this.bloqueado = bloqueado;
            this.tentativasFalhas = 0;
        }
    }

    static class Fornecedor {
        String codigo;
        String nome;
        String cnpj;
        String itensFornecidos;

        Fornecedor(String codigo, String nome, String cnpj, String itensFornecidos) {
            this.codigo = codigo;
            this.nome = nome;
            this.cnpj = cnpj;
            this.itensFornecidos = itensFornecidos;
        }
    }

    static class Produto {
        String codigo;
        String nome;
        String ncm;
        double precoUnitario;
        int estoque;
        double aliquotaImposto; // Ex.: 0.18 = 18%
        String codigoFornecedor;

        Produto(String codigo, String nome, String ncm, double precoUnitario,
                int estoque, double aliquotaImposto, String codigoFornecedor) {
            this.codigo = codigo;
            this.nome = nome;
            this.ncm = ncm;
            this.precoUnitario = precoUnitario;
            this.estoque = estoque;
            this.aliquotaImposto = aliquotaImposto;
            this.codigoFornecedor = codigoFornecedor;
        }
    }

    static class ItemCarrinho {
        final String codigoProduto;
        final String nomeProduto;
        final String ncm;
        final double precoUnitario;
        int quantidade;
        final double aliquotaImposto;

        ItemCarrinho(Produto produto, int quantidade) {
            this.codigoProduto = produto.codigo;
            this.nomeProduto = produto.nome;
            this.ncm = produto.ncm;
            this.precoUnitario = produto.precoUnitario;
            this.quantidade = quantidade;
            this.aliquotaImposto = produto.aliquotaImposto;
        }

        double subtotal() {
            return precoUnitario * quantidade;
        }

        double valorImposto() {
            return subtotal() * aliquotaImposto;
        }

        double totalItem() {
            return subtotal() + valorImposto();
        }
    }

    static class MovimentoFinanceiro {
        String dataHora;
        String tipo; // VENDA, ESTORNO, AJUSTE_SALARIO, ENTRADA, SAIDA
        String origem;
        String descricao;
        double valor;
        double saldoApos;

        MovimentoFinanceiro(String dataHora, String tipo, String origem,
                            String descricao, double valor, double saldoApos) {
            this.dataHora = dataHora;
            this.tipo = tipo;
            this.origem = origem;
            this.descricao = descricao;
            this.valor = valor;
            this.saldoApos = saldoApos;
        }
    }

    static class RegistroVenda {
        int numeroNF;
        String dataHora;
        String clienteUR;
        String operadorUR;
        String resumoItens;
        double subtotal;
        double impostos;
        double total;

        RegistroVenda(int numeroNF, String dataHora, String clienteUR, String operadorUR,
                      String resumoItens, double subtotal, double impostos, double total) {
            this.numeroNF = numeroNF;
            this.dataHora = dataHora;
            this.clienteUR = clienteUR;
            this.operadorUR = operadorUR;
            this.resumoItens = resumoItens;
            this.subtotal = subtotal;
            this.impostos = impostos;
            this.total = total;
        }
    }

    // ==================== AUTORIDADES ====================
    enum Permissao {
        SUPERVISAO_GERAL,
        GERIR_PESSOAS_DEMITIR_RECONTRATAR,
        GERIR_SENHAS_E_BLOQUEIOS,
        VER_E_AJUSTAR_SALARIOS,
        VER_MOVIMENTACAO_E_HISTORICO_DINHEIRO,
        MOVIMENTAR_ESTOQUE_E_PRODUTOS,
        VER_FORNECEDORES_E_FORNECIMENTO,
        REGISTRAR_VENDA_E_VER_REGISTRO_VENDAS,
        VER_CLIENTES_CADASTRADOS
    }

    enum Cargo {
        GESTAO(EnumSet.allOf(Permissao.class)),
        RH(EnumSet.of(
                Permissao.GERIR_PESSOAS_DEMITIR_RECONTRATAR,
                Permissao.GERIR_SENHAS_E_BLOQUEIOS,
                Permissao.VER_CLIENTES_CADASTRADOS
        )),
        FINANCEIRO(EnumSet.of(
                Permissao.VER_E_AJUSTAR_SALARIOS,
                Permissao.VER_MOVIMENTACAO_E_HISTORICO_DINHEIRO,
                Permissao.REGISTRAR_VENDA_E_VER_REGISTRO_VENDAS
        )),
        ESTOQUE(EnumSet.of(
                Permissao.MOVIMENTAR_ESTOQUE_E_PRODUTOS,
                Permissao.VER_FORNECEDORES_E_FORNECIMENTO
        )),
        CAIXA(EnumSet.of(
                Permissao.REGISTRAR_VENDA_E_VER_REGISTRO_VENDAS,
                Permissao.VER_CLIENTES_CADASTRADOS
        ));

        private final Set<Permissao> permissoes;

        Cargo(Set<Permissao> permissoes) {
            this.permissoes = permissoes;
        }

        boolean pode(Permissao p) {
            return permissoes.contains(p);
        }

        static Cargo deTexto(String texto) {
            if (texto == null) return CAIXA;
            try {
                return Cargo.valueOf(texto.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return CAIXA;
            }
        }
    }

    // ==================== ESTADO EM MEMORIA ====================
    private static double saldoCaixa = 185000.0;
    private static int sequencialNF = 1001;
    private static final List<Funcionario> funcionarios = new ArrayList<>();
    private static final List<Cliente> clientes = new ArrayList<>();
    private static final List<Fornecedor> fornecedores = new ArrayList<>();
    private static final List<Produto> produtos = new ArrayList<>();
    private static final List<MovimentoFinanceiro> movimentosFinanceiros = new ArrayList<>();
    private static final List<RegistroVenda> vendas = new ArrayList<>();
    private static final Map<String, String> tabelaFuncSenha = new LinkedHashMap<>();

    // ==================== FORMATACAO E LOGS ====================
    private static String agoraTexto() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }

    private static String agoraArquivo() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMMyyyy_HHmmss"));
    }

    private static String formatarBRL(double valor) {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(new Locale("pt", "BR"));
        sym.setGroupingSeparator('.');
        sym.setDecimalSeparator(',');
        return new DecimalFormat("R$ #,##0.00", sym).format(valor);
    }

    private static void registrarLogIO(String direcao, String origem, String detalhe) {
        String linha = agoraTexto() + ";" + direcao + ";" + origem + ";" + detalhe.replace(";", ",");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(ARQUIVO_LOG_IO, true))) {
            bw.write(linha);
            bw.newLine();
        } catch (IOException ignored) {
        }
    }

    private static void registrarAuditoria(String ator, String evento) {
        String linha = agoraTexto() + " | " + ator + " | " + evento.replace(";", ",");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(ARQUIVO_AUDITORIA, true))) {
            bw.write(linha);
            bw.newLine();
        } catch (IOException ignored) {
        }
    }

    // ==================== MODULO DE VALIDACAO ====================
    private static void checarVoltar(String texto) {
        if (texto == null) return;
        String cmd = texto.trim().toUpperCase(Locale.ROOT);
        if ("VOLTAR".equals(cmd) || "CANCELAR".equals(cmd)) {
            System.out.println("[VOLTAR] Operacao cancelada. Retornando para a tela anterior...");
            throw new RetornarTelaException("Retorno solicitado");
        }
    }

    private static int lerInteiroPositivo(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt + " [ou VOLTAR]: ");
            String raw = sc.nextLine();
            checarVoltar(raw);

            String s = raw.trim();
            if (s.isEmpty()) {
                System.out.println("[AVISO] Campo vazio. Digite um numero inteiro positivo ou VOLTAR.");
                continue;
            }
            if (s.startsWith("-") || s.contains(".") || s.contains(",")) {
                System.out.println("[AVISO] Nao sao permitidos '-0', negativos ou numeros quebrados ('" + s + "').");
                continue;
            }
            if (!s.matches("\\d+")) {
                System.out.println("[AVISO] Digite apenas digitos inteiros, sem letras ou caracteres especiais ('" + s + "').");
                continue;
            }
            try {
                int v = Integer.parseInt(s);
                if (v <= 0) {
                    System.out.println("[AVISO] O numero deve ser maior que zero.");
                    continue;
                }
                return v;
            } catch (NumberFormatException e) {
                System.out.println("[AVISO] Numero fora do intervalo permitido.");
            }
        }
    }

    private static int lerOpcaoMenu(Scanner sc, String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt + " (" + min + " a " + max + ", ou VOLTAR): ");
            String raw = sc.nextLine();
            checarVoltar(raw);

            String s = raw.trim();
            if (s.isEmpty()) {
                System.out.println("[AVISO] Opcao vazia. Informe um numero de " + min + " a " + max + ".");
                continue;
            }
            if (s.startsWith("-") || s.contains(".") || s.contains(",")) {
                System.out.println("[AVISO] Opcao invalida ('" + s + "'). Nao use '-0', negativos ou numeros quebrados.");
                continue;
            }
            if (!s.matches("\\d+")) {
                System.out.println("[AVISO] Digite apenas o numero da opcao ('" + s + "').");
                continue;
            }
            try {
                int op = Integer.parseInt(s);
                if (op < min || op > max) {
                    System.out.println("[AVISO] Opcao fora da lista (" + op + "). Escolha de " + min + " a " + max + ".");
                    continue;
                }
                return op;
            } catch (NumberFormatException e) {
                System.out.println("[AVISO] Opcao numerica invalida.");
            }
        }
    }

    private static double lerDecimalPositivo(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt + " [ou VOLTAR]: ");
            String raw = sc.nextLine();
            checarVoltar(raw);

            String s = raw.trim().replace(",", ".");
            if (s.isEmpty()) {
                System.out.println("[AVISO] Valor vazio. Digite um numero maior que zero.");
                continue;
            }
            if (s.startsWith("-")) {
                System.out.println("[AVISO] Valores negativos ou '-0' nao sao permitidos ('" + raw.trim() + "').");
                continue;
            }
            if (!s.matches("\\d+(\\.\\d+)?")) {
                System.out.println("[AVISO] Valor quebrado ou invalido ('" + raw.trim() + "'). Use apenas numeros.");
                continue;
            }
            try {
                double d = Double.parseDouble(s);
                if (Double.isNaN(d) || Double.isInfinite(d) || d <= 0.0) {
                    System.out.println("[AVISO] O valor deve ser maior que zero.");
                    continue;
                }
                return d;
            } catch (NumberFormatException e) {
                System.out.println("[AVISO] Valor numerico invalido.");
            }
        }
    }

    private static String lerDaLista(Scanner sc, String rotulo, String... permitidos) {
        List<String> lista = Arrays.asList(permitidos);
        while (true) {
            System.out.print(rotulo + " " + lista + " [ou VOLTAR]: ");
            String raw = sc.nextLine();
            checarVoltar(raw);

            String val = raw.trim().toUpperCase(Locale.ROOT);
            if (lista.contains(val)) {
                return val;
            }
            System.out.println("[AVISO] Valor '" + raw.trim() + "' fora da lista. Escolha entre: " + lista);
        }
    }

    private static String lerNomeSemNumeros(Scanner sc, String rotulo) {
        while (true) {
            System.out.print(rotulo + " [ou VOLTAR]: ");
            String raw = sc.nextLine();
            checarVoltar(raw);

            String nome = raw.trim();
            if (nome.length() < 3) {
                System.out.println("[AVISO] Informe pelo menos 3 caracteres.");
                continue;
            }
            if (nome.contains(";") || !nome.matches("[A-Za-zÀ-ÿ\\s]+")) {
                System.out.println("[AVISO] Nao sao permitidos numeros, ';' ou caracteres especiais.");
                continue;
            }
            return nome.toUpperCase(Locale.ROOT);
        }
    }

    private static String lerURValida(Scanner sc, String rotulo) {
        while (true) {
            System.out.print(rotulo + " [ou VOLTAR]: ");
            String raw = sc.nextLine();
            checarVoltar(raw);

            String ur = raw.trim().toUpperCase(Locale.ROOT);
            if (ur.length() < 2 || ur.length() > 20 || !ur.matches("[A-Z0-9-]+")) {
                System.out.println("[AVISO] Use apenas letras, numeros ou '-' (2 a 20 chars, sem espacos ou ';').");
                continue;
            }
            return ur;
        }
    }

    private static String lerCPFValido(Scanner sc) {
        while (true) {
            System.out.print("CPF (11 digitos) [ou VOLTAR]: ");
            String raw = sc.nextLine();
            checarVoltar(raw);

            String limpo = raw.trim().replace(".", "").replace("-", "").replace(" ", "");
            if (!limpo.matches("\\d{11}") || limpo.chars().distinct().count() == 1) {
                System.out.println("[AVISO] CPF invalido. Digite 11 digitos numericos sem letras ou sequencia repetida.");
                continue;
            }
            int s1 = 0;
            for (int i = 0; i < 9; i++) s1 += (limpo.charAt(i) - '0') * (10 - i);
            int d1 = 11 - (s1 % 11);
            if (d1 >= 10) d1 = 0;

            int s2 = 0;
            for (int i = 0; i < 10; i++) s2 += (limpo.charAt(i) - '0') * (11 - i);
            int d2 = 11 - (s2 % 11);
            if (d2 >= 10) d2 = 0;

            if (d1 == (limpo.charAt(9) - '0') && d2 == (limpo.charAt(10) - '0')) {
                return limpo;
            }
            System.out.println("[AVISO] Digitos verificadores do CPF nao conferem.");
        }
    }

    private static String lerSenhaValida(Scanner sc) {
        while (true) {
            System.out.print("Senha (4 a 32 chars, sem espacos ou ';') [ou VOLTAR]: ");
            String raw = sc.nextLine();
            checarVoltar(raw);

            if (raw.isEmpty() || raw.contains(";") || raw.matches(".*\\s.*") || raw.length() < 4 || raw.length() > 32) {
                System.out.println("[AVISO] Senha invalida. Use 4 a 32 caracteres sem espacos ou ';'.");
                continue;
            }
            return raw;
        }
    }

    // ==================== PERSISTENCIA (.TXT) E BACKUP AUTOMATICO ====================
    private static void popularDadosPadrao() {
        funcionarios.clear();
        clientes.clear();
        fornecedores.clear();
        produtos.clear();
        movimentosFinanceiros.clear();
        vendas.clear();
        tabelaFuncSenha.clear();

        saldoCaixa = 185000.0;
        sequencialNF = 1001;

        funcionarios.add(new Funcionario("UR-001", "MARIANA SILVA", "11144477735", "GESTAO", "gest123", 11000.0, false, "ATIVO"));
        funcionarios.add(new Funcionario("UR-002", "FERNANDA ALMEIDA", "33366699917", "RH", "rh123", 6200.0, false, "ATIVO"));
        funcionarios.add(new Funcionario("UR-003", "CARLOS SOUZA", "22255588809", "FINANCEIRO", "fin123", 6800.0, false, "ATIVO"));
        funcionarios.add(new Funcionario("UR-004", "RODRIGO ALMEIDA", "44477711125", "ESTOQUE", "est123", 4800.0, false, "ATIVO"));
        funcionarios.add(new Funcionario("UR-005", "LUCAS PEREIRA", "55588822233", "CAIXA", "cx123", 3900.0, false, "ATIVO"));
        for (Funcionario f : funcionarios) {
            tabelaFuncSenha.put(f.ur, f.senha);
        }

        clientes.add(new Cliente("CLI-001", "ANA BEATRIZ COSTA", "66699933341", "cli123", false));
        clientes.add(new Cliente("CLI-002", "RAFAEL OLIVEIRA", "77711144450", "cli456", false));

        fornecedores.add(new Fornecedor("FOR-001", "DISTRIBUIDORA CITRUS LTDA", "11222333000181", "PRD-001,PRD-002"));
        fornecedores.add(new Fornecedor("FOR-002", "CEREAIS E GRAOS BRASIL SA", "33444555000192", "PRD-003"));

        produtos.add(new Produto("PRD-001", "SUCO INTEGRAL LARANJA 1L", "20091900", 18.50, 120, 0.18, "FOR-001"));
        produtos.add(new Produto("PRD-002", "CAFE TORRADO E MOIDO 500G", "09012100", 22.50, 80, 0.12, "FOR-001"));
        produtos.add(new Produto("PRD-003", "ARROZ AGULHINHA TIPO 1 5KG", "10063021", 27.90, 95, 0.07, "FOR-002"));
    }

    private static void sincronizarMemoria() {
        for (Funcionario f : funcionarios) {
            String s = tabelaFuncSenha.get(f.ur);
            if (s != null && !s.isEmpty()) {
                f.senha = s;
            } else {
                tabelaFuncSenha.put(f.ur, f.senha);
            }
        }
    }

    private static void carregarDadosIniciais() {
        File arq = new File(ARQUIVO_DADOS);
        if (!arq.exists()) {
            popularDadosPadrao();
            salvarArquivo(ARQUIVO_DADOS, "Carga inicial padrao");
            return;
        }

        funcionarios.clear();
        clientes.clear();
        fornecedores.clear();
        produtos.clear();
        movimentosFinanceiros.clear();
        vendas.clear();
        tabelaFuncSenha.clear();

        String secao = "";
        try (BufferedReader br = new BufferedReader(new FileReader(arq))) {
            String linha;
            while ((linha = br.readLine()) != null) {
                linha = linha.trim();
                if (linha.isEmpty() || linha.startsWith("#")) continue;
                if (linha.startsWith("[") && linha.endsWith("]")) {
                    if (linha.endsWith("_INICIO]")) {
                        secao = linha.substring(1, linha.length() - "_INICIO]".length());
                    } else if (linha.endsWith("_FIM]")) {
                        secao = "";
                    }
                    continue;
                }

                String[] p = linha.split(";");
                switch (secao) {
                    case "CAIXA":
                        if (linha.startsWith("SALDO=")) {
                            saldoCaixa = Double.parseDouble(linha.substring("SALDO=".length()).trim());
                        } else if (linha.startsWith("SEQ_NF=")) {
                            sequencialNF = Integer.parseInt(linha.substring("SEQ_NF=".length()).trim());
                        }
                        break;
                    case "FUNCIONARIOS":
                        if (p.length >= 8) {
                            Funcionario f = new Funcionario(
                                    p[0].trim(), p[1].trim(), p[2].trim(), p[3].trim(),
                                    p[4].trim(), Double.parseDouble(p[5].trim()),
                                    Boolean.parseBoolean(p[6].trim()), p[7].trim()
                            );
                            funcionarios.add(f);
                            tabelaFuncSenha.put(f.ur, f.senha);
                        }
                        break;
                    case "CLIENTES":
                        if (p.length >= 5) {
                            clientes.add(new Cliente(
                                    p[0].trim(), p[1].trim(), p[2].trim(), p[3].trim(),
                                    Boolean.parseBoolean(p[4].trim())
                            ));
                        }
                        break;
                    case "FORNECEDORES":
                        if (p.length >= 4) {
                            fornecedores.add(new Fornecedor(p[0].trim(), p[1].trim(), p[2].trim(), p[3].trim()));
                        }
                        break;
                    case "PRODUTOS":
                        if (p.length >= 7) {
                            produtos.add(new Produto(
                                    p[0].trim(), p[1].trim(), p[2].trim(),
                                    Double.parseDouble(p[3].trim()), Integer.parseInt(p[4].trim()),
                                    Double.parseDouble(p[5].trim()), p[6].trim()
                            ));
                        }
                        break;
                    case "MOVIMENTOS_FINANCEIROS":
                        if (p.length >= 6) {
                            movimentosFinanceiros.add(new MovimentoFinanceiro(
                                    p[0].trim(), p[1].trim(), p[2].trim(), p[3].trim(),
                                    Double.parseDouble(p[4].trim()), Double.parseDouble(p[5].trim())
                            ));
                        }
                        break;
                    case "VENDAS":
                        if (p.length >= 8) {
                            vendas.add(new RegistroVenda(
                                    Integer.parseInt(p[0].trim()), p[1].trim(), p[2].trim(), p[3].trim(),
                                    p[4].trim(), Double.parseDouble(p[5].trim()),
                                    Double.parseDouble(p[6].trim()), Double.parseDouble(p[7].trim())
                            ));
                        }
                        break;
                    default:
                        break;
                }
            }
            registrarLogIO("ENTRADA_DADOS", "Sistema", "Carregado " + ARQUIVO_DADOS);
        } catch (IOException | NumberFormatException e) {
            popularDadosPadrao();
            registrarAuditoria("SISTEMA", "Falha ao ler " + ARQUIVO_DADOS + ": " + e.getMessage());
        }
    }

    private static boolean salvarArquivo(String nomeArquivo, String desc) {
        sincronizarMemoria();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(nomeArquivo), false))) {
            bw.write("# " + desc + " - " + agoraTexto());
            bw.newLine();

            bw.write("[CAIXA_INICIO]"); bw.newLine();
            bw.write("SALDO=" + saldoCaixa); bw.newLine();
            bw.write("SEQ_NF=" + sequencialNF); bw.newLine();
            bw.write("[CAIXA_FIM]"); bw.newLine();

            bw.write("[FUNCIONARIOS_INICIO]"); bw.newLine();
            for (Funcionario f : funcionarios) {
                bw.write(f.ur + ";" + f.nome + ";" + f.cpf + ";" + f.cargo + ";" + f.senha + ";"
                        + f.salario + ";" + f.bloqueado + ";" + f.statusRH);
                bw.newLine();
            }
            bw.write("[FUNCIONARIOS_FIM]"); bw.newLine();

            bw.write("[CLIENTES_INICIO]"); bw.newLine();
            for (Cliente c : clientes) {
                bw.write(c.ur + ";" + c.nome + ";" + c.cpf + ";" + c.senha + ";" + c.bloqueado);
                bw.newLine();
            }
            bw.write("[CLIENTES_FIM]"); bw.newLine();

            bw.write("[FORNECEDORES_INICIO]"); bw.newLine();
            for (Fornecedor fn : fornecedores) {
                bw.write(fn.codigo + ";" + fn.nome + ";" + fn.cnpj + ";" + fn.itensFornecidos);
                bw.newLine();
            }
            bw.write("[FORNECEDORES_FIM]"); bw.newLine();

            bw.write("[PRODUTOS_INICIO]"); bw.newLine();
            for (Produto p : produtos) {
                bw.write(p.codigo + ";" + p.nome + ";" + p.ncm + ";" + p.precoUnitario + ";"
                        + p.estoque + ";" + p.aliquotaImposto + ";" + p.codigoFornecedor);
                bw.newLine();
            }
            bw.write("[PRODUTOS_FIM]"); bw.newLine();

            bw.write("[MOVIMENTOS_FINANCEIROS_INICIO]"); bw.newLine();
            for (MovimentoFinanceiro m : movimentosFinanceiros) {
                bw.write(m.dataHora + ";" + m.tipo + ";" + m.origem + ";" + m.descricao + ";"
                        + m.valor + ";" + m.saldoApos);
                bw.newLine();
            }
            bw.write("[MOVIMENTOS_FINANCEIROS_FIM]"); bw.newLine();

            bw.write("[VENDAS_INICIO]"); bw.newLine();
            for (RegistroVenda v : vendas) {
                bw.write(v.numeroNF + ";" + v.dataHora + ";" + v.clienteUR + ";" + v.operadorUR + ";"
                        + v.resumoItens + ";" + v.subtotal + ";" + v.impostos + ";" + v.total);
                bw.newLine();
            }
            bw.write("[VENDAS_FIM]"); bw.newLine();

            bw.flush();
            registrarLogIO("SAIDA_DADOS", "Sistema", "Gravado " + nomeArquivo);
            return true;
        } catch (IOException e) {
            registrarAuditoria("SISTEMA", "Erro ao gravar " + nomeArquivo + ": " + e.getMessage());
            return false;
        }
    }

    private static synchronized void executarSincronizacaoEBackupAoFechar(boolean exibirConsole, String motivo) {
        if (encerramentoExecutado) return;
        encerramentoExecutado = true;

        sincronizarMemoria();
        String arqFechamento = "backup_fechamento_" + agoraArquivo() + ".txt";

        boolean ok1 = salvarArquivo(ARQUIVO_DADOS, "Sincronizacao principal (" + motivo + ")");
        boolean ok2 = salvarArquivo(ARQUIVO_BACKUP, "Backup editavel (" + motivo + ")");
        boolean ok3 = salvarArquivo(arqFechamento, "Backup fechamento (" + motivo + ")");

        registrarLogIO("SAIDA_DADOS", "Sistema", "Backup automatico [" + motivo + "] -> " + ARQUIVO_BACKUP + " e " + arqFechamento);
        registrarAuditoria("SISTEMA", "Encerramento (" + motivo + "): dados=" + ok1 + ", backup=" + ok2 + ", fechamento=" + ok3);

        if (exibirConsole) {
            System.out.println("\n[OK] Backups sincronizados automaticamente (" + motivo + "): "
                    + ARQUIVO_DADOS + ", " + ARQUIVO_BACKUP + ", " + arqFechamento);
        }
    }

    private static void registrarShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> executarSincronizacaoEBackupAoFechar(false, "SHUTDOWN_HOOK")));
    }

    // ==================== AUXILIARES DE PRODUTO ====================
    private static Produto buscarProdutoOficial(String codigo) {
        if (codigo == null) return null;
        for (Produto p : produtos) {
            if (p.codigo.equalsIgnoreCase(codigo.trim())) {
                return p;
            }
        }
        return null;
    }

    private static void listarProdutosComImposto() {
        System.out.println("\nCOD     | PRODUTO                        | PRECO UNIT  | IMPOSTO | ESTOQUE | FORN");
        System.out.println("--------+--------------------------------+-------------+---------+---------+--------");
        for (Produto p : produtos) {
            System.out.printf("%-7s | %-30s | %11s | %6.1f%% | %7d | %-7s%n",
                    p.codigo, p.nome, formatarBRL(p.precoUnitario), p.aliquotaImposto * 100, p.estoque, p.codigoFornecedor);
        }
    }

    // ==================== MODULO DO CLIENTE ====================
    private static Cliente cadastrarCliente(Scanner sc) {
        try {
            System.out.println("\n--- [CLIENTE] NOVO CADASTRO ---");
            String nome = lerNomeSemNumeros(sc, "Nome completo");
            String cpf = lerCPFValido(sc);
            for (Cliente c : clientes) {
                if (c.cpf.equals(cpf)) {
                    System.out.println("[AVISO] CPF ja cadastrado (" + c.ur + ").");
                    return null;
                }
            }
            String senha = lerSenhaValida(sc);
            String ur = String.format("CLI-%03d", clientes.size() + 1);
            Cliente novo = new Cliente(ur, nome, cpf, senha, false);
            clientes.add(novo);
            registrarLogIO("ENTRADA_DADOS", novo.nome, "Novo cliente cadastrado (" + novo.ur + ")");
            registrarAuditoria(novo.nome, "Cliente cadastrado (" + novo.ur + ")");
            System.out.println("[OK] Cadastro concluido! Sua UR de acesso e: " + novo.ur);
            return novo;
        } catch (RetornarTelaException e) {
            return null;
        }
    }

    private static Cliente loginCliente(Scanner sc) {
        try {
            System.out.println("\n--- [CLIENTE] LOGIN ---");
            String ur = lerURValida(sc, "UR do cliente");
            Cliente alvo = null;
            for (Cliente c : clientes) {
                if (c.ur.equalsIgnoreCase(ur)) {
                    alvo = c;
                    break;
                }
            }
            if (alvo == null) {
                System.out.println("[AVISO] Cliente nao encontrado.");
                return null;
            }
            if (alvo.bloqueado) {
                System.out.println("[AVISO] Conta de cliente bloqueada.");
                return null;
            }
            final int MAX = 3;
            while (alvo.tentativasFalhas < MAX) {
                System.out.print("Senha (tentativa " + (alvo.tentativasFalhas + 1) + " de " + MAX + ") [ou VOLTAR]: ");
                String raw = sc.nextLine();
                checarVoltar(raw);
                if (raw.equals(alvo.senha)) {
                    alvo.tentativasFalhas = 0;
                    registrarLogIO("ENTRADA_DADOS", alvo.nome + " (" + alvo.ur + ")", "Login de cliente");
                    return alvo;
                }
                alvo.tentativasFalhas++;
                int rest = MAX - alvo.tentativasFalhas;
                if (rest > 0) {
                    System.out.println("[AVISO] Senha incorreta. Tentativas restantes: " + rest);
                } else {
                    alvo.bloqueado = true;
                    System.out.println("[AVISO] Limite atingido. Conta bloqueada.");
                }
            }
            return null;
        } catch (RetornarTelaException e) {
            return null;
        }
    }

    private static void exibirCarrinhoComPrecos(List<ItemCarrinho> carrinho) {
        if (carrinho.isEmpty()) {
            System.out.println("\n[CARRINHO VAZIO] Nenhum item adicionado. Valor total: " + formatarBRL(0.0));
            return;
        }
        double subTotal = 0.0, totalImp = 0.0, totalGeral = 0.0;
        System.out.println("\n==================== CARRINHO DE COMPRAS ====================");
        System.out.printf("%-7s | %-24s | %4s | %11s | %10s | %11s%n",
                "COD", "PRODUTO", "QTD", "PRECO UNIT", "IMPOSTO", "TOTAL ITEM");
        System.out.println("--------+--------------------------+------+-------------+------------+------------");
        for (ItemCarrinho item : carrinho) {
            double sub = item.subtotal();
            double imp = item.valorImposto();
            double tot = item.totalItem();
            subTotal += sub;
            totalImp += imp;
            totalGeral += tot;
            String nomeCurto = item.nomeProduto.length() > 24 ? item.nomeProduto.substring(0, 24) : item.nomeProduto;
            System.out.printf("%-7s | %-24s | %4d | %11s | %10s | %11s%n",
                    item.codigoProduto, nomeCurto, item.quantidade,
                    formatarBRL(item.precoUnitario), formatarBRL(imp), formatarBRL(tot));
        }
        System.out.println("-------------------------------------------------------------");
        System.out.println("Subtotal sem impostos : " + formatarBRL(subTotal));
        System.out.println("Total de impostos     : " + formatarBRL(totalImp));
        System.out.println("PRECO TOTAL DO CARRINHO: " + formatarBRL(totalGeral));
        System.out.println("=============================================================");
    }

    private static ItemCarrinho localizarItemCarrinho(List<ItemCarrinho> carrinho, String codigo) {
        for (ItemCarrinho item : carrinho) {
            if (item.codigoProduto.equalsIgnoreCase(codigo)) {
                return item;
            }
        }
        return null;
    }

    private static void gerenciarCarrinhoCliente(Scanner sc, List<ItemCarrinho> carrinho) {
        boolean ativo = true;
        while (ativo) {
            exibirCarrinhoComPrecos(carrinho);
            System.out.println("\n--- GERENCIAR CARRINHO ---");
            System.out.println("1 - Adicionar item");
            System.out.println("2 - Alterar quantidade de item");
            System.out.println("3 - Retirar item");
            System.out.println("0 - Voltar ao painel do cliente");

            try {
                int op = lerOpcaoMenu(sc, "Opcao", 0, 3);
                if (op == 0) {
                    ativo = false;
                } else if (op == 1) {
                    listarProdutosComImposto();
                    String cod = lerURValida(sc, "Codigo do produto para adicionar");
                    Produto prod = buscarProdutoOficial(cod);
                    if (prod == null) {
                        System.out.println("[AVISO - ITEM NAO REGISTRADO] Codigo '" + cod + "' nao consta nos produtos cadastrados.");
                        continue;
                    }
                    if (prod.estoque <= 0) {
                        System.out.println("[AVISO - SEM ESTOQUE] Produto '" + prod.nome + "' sem estoque disponivel.");
                        continue;
                    }
                    ItemCarrinho existente = localizarItemCarrinho(carrinho, prod.codigo);
                    int jaNoCarrinho = (existente != null) ? existente.quantidade : 0;
                    int disponivel = prod.estoque - jaNoCarrinho;
                    if (disponivel <= 0) {
                        System.out.println("[AVISO - ESTOQUE INSUFICIENTE] Todo o estoque (" + prod.estoque + " un.) ja esta no carrinho.");
                        continue;
                    }
                    int qtd = lerInteiroPositivo(sc, "Quantidade a adicionar (max " + disponivel + ")");
                    if (qtd > disponivel) {
                        System.out.println("[AVISO - ESTOQUE INSUFICIENTE] Solicitado: " + qtd + " | Maximo adicional: " + disponivel);
                        continue;
                    }
                    if (existente != null) {
                        existente.quantidade += qtd;
                    } else {
                        carrinho.add(new ItemCarrinho(prod, qtd));
                    }
                    System.out.println("[OK] Item adicionado ao carrinho.");
                } else if (op == 2) {
                    if (carrinho.isEmpty()) {
                        System.out.println("[AVISO] Carrinho vazio.");
                        continue;
                    }
                    String cod = lerURValida(sc, "Codigo do item no carrinho");
                    ItemCarrinho item = localizarItemCarrinho(carrinho, cod);
                    if (item == null) {
                        System.out.println("[AVISO] Item nao encontrado no carrinho.");
                        continue;
                    }
                    Produto oficial = buscarProdutoOficial(item.codigoProduto);
                    if (oficial == null) {
                        System.out.println("[AVISO - ITEM NAO REGISTRADO] Produto nao existe mais no catalogo.");
                        continue;
                    }
                    int novaQtd = lerInteiroPositivo(sc, "Nova quantidade (estoque: " + oficial.estoque + ")");
                    if (novaQtd > oficial.estoque) {
                        System.out.println("[AVISO - ESTOQUE INSUFICIENTE] Disponivel: " + oficial.estoque + " un.");
                        continue;
                    }
                    item.quantidade = novaQtd;
                    System.out.println("[OK] Quantidade atualizada para " + novaQtd + " un.");
                } else if (op == 3) {
                    if (carrinho.isEmpty()) {
                        System.out.println("[AVISO] Carrinho vazio.");
                        continue;
                    }
                    String cod = lerURValida(sc, "Codigo do item para retirar");
                    ItemCarrinho item = localizarItemCarrinho(carrinho, cod);
                    if (item == null) {
                        System.out.println("[AVISO] Item nao encontrado no carrinho.");
                        continue;
                    }
                    carrinho.remove(item);
                    System.out.println("[OK] Item '" + item.nomeProduto + "' removido.");
                }
            } catch (RetornarTelaException ignored) {
                ativo = false;
            }
        }
    }

    private static void finalizarCompraCliente(Cliente cliente, List<ItemCarrinho> carrinho) {
        if (carrinho.isEmpty()) {
            System.out.println("[AVISO] Carrinho vazio. Adicione itens antes de finalizar.");
            return;
        }

        // Checagem final de itens registrados e estoque disponivel antes de emitir a NF
        List<String> problemas = new ArrayList<>();
        for (ItemCarrinho item : carrinho) {
            Produto oficial = buscarProdutoOficial(item.codigoProduto);
            if (oficial == null) {
                problemas.add("Item '" + item.codigoProduto + "' (" + item.nomeProduto + ") nao esta registrado.");
            } else if (item.quantidade <= 0) {
                problemas.add("Quantidade invalida para '" + oficial.codigo + "'.");
            } else if (item.quantidade > oficial.estoque) {
                problemas.add("Estoque insuficiente para '" + oficial.codigo + "' (pedido: "
                        + item.quantidade + ", disponivel: " + oficial.estoque + ").");
            }
        }

        if (!problemas.isEmpty()) {
            System.out.println("\n[BLOQUEIO - NOTA FISCAL NAO EMITIDA]");
            for (String p : problemas) {
                System.out.println("  - " + p);
            }
            System.out.println("[ACAO] Ajuste o carrinho e tente novamente.");
            return;
        }

        double sub = 0.0, imp = 0.0;
        StringBuilder resumo = new StringBuilder();
        for (ItemCarrinho item : carrinho) {
            Produto oficial = buscarProdutoOficial(item.codigoProduto);
            oficial.estoque -= item.quantidade;
            double subItem = oficial.precoUnitario * item.quantidade;
            double impItem = subItem * oficial.aliquotaImposto;
            sub += subItem;
            imp += impItem;
            if (resumo.length() > 0) resumo.append(", ");
            resumo.append(item.quantidade).append("x ").append(oficial.codigo);
        }

        double total = sub + imp;
        int numNF = sequencialNF++;
        String dh = agoraTexto();
        saldoCaixa += total;

        vendas.add(new RegistroVenda(numNF, dh, cliente.ur, "AUTO-CLIENTE", resumo.toString(), sub, imp, total));
        movimentosFinanceiros.add(new MovimentoFinanceiro(dh, "VENDA", cliente.ur, "NF #" + numNF, total, saldoCaixa));

        String nfTexto = "==================== NOTA FISCAL #" + numNF + " ====================\n"
                + "Data/Hora : " + dh + "\n"
                + "Cliente   : " + cliente.nome + " (" + cliente.ur + " | CPF: " + cliente.cpf + ")\n"
                + "Itens     : " + resumo + "\n"
                + "Subtotal  : " + formatarBRL(sub) + "\n"
                + "Impostos  : " + formatarBRL(imp) + "\n"
                + "Total NF  : " + formatarBRL(total) + "\n"
                + "===============================================================\n";

        System.out.println("\n" + nfTexto);
        String arqNF = "nota_fiscal_" + numNF + ".txt";
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(arqNF))) {
            bw.write(nfTexto);
            bw.flush();
            registrarLogIO("SAIDA_DADOS", cliente.nome, "Nota Fiscal #" + numNF + " gravada em " + arqNF);
            registrarAuditoria(cliente.nome, "Compra finalizada NF #" + numNF + " (" + formatarBRL(total) + ")");
        } catch (IOException e) {
            System.out.println("[AVISO] Nao foi possivel salvar " + arqNF);
        }

        carrinho.clear();
        System.out.println("[OK] Nota Fiscal #" + numNF + " emitida. Retornando ao inicio do painel do cliente...");
    }

    private static void painelCliente(Scanner sc, Cliente cliente) {
        List<ItemCarrinho> carrinho = new ArrayList<>();
        boolean ativo = true;

        while (ativo) {
            double sub = 0.0, imp = 0.0;
            for (ItemCarrinho item : carrinho) {
                sub += item.subtotal();
                imp += item.valorImposto();
            }
            System.out.println("\n=======================================================");
            System.out.println("PAINEL DO CLIENTE - " + cliente.nome + " [" + cliente.ur + "]");
            System.out.println("Carrinho: " + carrinho.size() + " | Sub: " + formatarBRL(sub)
                    + " | Imp: " + formatarBRL(imp) + " | Total: " + formatarBRL(sub + imp));
            System.out.println("=======================================================");
            System.out.println("1 - Ver produtos e impostos");
            System.out.println("2 - Exibir o que esta no carrinho e o preco");
            System.out.println("3 - Gerenciar carrinho (adicionar, alterar qtd, retirar)");
            System.out.println("4 - Finalizar compra e emitir Nota Fiscal");
            System.out.println("5 - Ver historico das minhas compras");
            System.out.println("0 - Sair do painel do cliente");

            try {
                int op = lerOpcaoMenu(sc, "Opcao", 0, 5);
                switch (op) {
                    case 1:
                        listarProdutosComImposto();
                        break;
                    case 2:
                        exibirCarrinhoComPrecos(carrinho);
                        break;
                    case 3:
                        gerenciarCarrinhoCliente(sc, carrinho);
                        break;
                    case 4:
                        finalizarCompraCliente(cliente, carrinho);
                        break;
                    case 5:
                        System.out.println("\n--- HISTORICO DE COMPRAS (" + cliente.ur + ") ---");
                        for (RegistroVenda v : vendas) {
                            if (v.clienteUR.equalsIgnoreCase(cliente.ur)) {
                                System.out.printf("NF #%d | %s | Itens: %s | Imp: %s | Total: %s%n",
                                        v.numeroNF, v.dataHora, v.resumoItens, formatarBRL(v.impostos), formatarBRL(v.total));
                            }
                        }
                        break;
                    case 0:
                        ativo = false;
                        break;
                }
            } catch (RetornarTelaException ignored) {
                // Retorna ao inicio do painel do cliente
            }
        }
    }

    // ==================== MODULO FUNCIONARIO (AUTORIDADES) ====================
    private static Funcionario loginFuncionario(Scanner sc) {
        try {
            System.out.println("\n--- [FUNCIONARIO] LOGIN ---");
            String ur = lerURValida(sc, "UR do funcionario");
            Funcionario alvo = null;
            for (Funcionario f : funcionarios) {
                if (f.ur.equalsIgnoreCase(ur)) {
                    alvo = f;
                    break;
                }
            }
            if (alvo == null) {
                System.out.println("[AVISO] Funcionario nao encontrado.");
                return null;
            }
            if ("DEMITIDO".equalsIgnoreCase(alvo.statusRH)) {
                System.out.println("[AVISO] Funcionario consta como DEMITIDO no RH. Acesso negado.");
                registrarAuditoria(alvo.nome, "Tentativa de login de funcionario DEMITIDO");
                return null;
            }
            if (alvo.bloqueado) {
                System.out.println("[AVISO] Conta BLOQUEADA. Solicite liberacao ao RH ou GESTAO.");
                return null;
            }
            final int MAX = 3;
            while (alvo.tentativasFalhas < MAX) {
                System.out.print("Senha (tentativa " + (alvo.tentativasFalhas + 1) + " de " + MAX + ") [ou VOLTAR]: ");
                String raw = sc.nextLine();
                checarVoltar(raw);
                if (raw.equals(alvo.senha)) {
                    alvo.tentativasFalhas = 0;
                    registrarLogIO("ENTRADA_DADOS", alvo.nome + " (" + alvo.ur + ")", "Login [" + alvo.cargo + "]");
                    registrarAuditoria(alvo.nome, "Login efetuado no cargo " + alvo.cargo);
                    return alvo;
                }
                alvo.tentativasFalhas++;
                int rest = MAX - alvo.tentativasFalhas;
                if (rest > 0) {
                    System.out.println("[AVISO] Senha incorreta. Tentativas restantes: " + rest);
                } else {
                    alvo.bloqueado = true;
                    System.out.println("[AVISO] Limite de tentativas atingido. Conta bloqueada.");
                }
            }
            return null;
        } catch (RetornarTelaException e) {
            return null;
        }
    }

    private static boolean autorizar(Funcionario f, Permissao p, String acao) {
        if (Cargo.deTexto(f.cargo).pode(p)) return true;
        System.out.println("[ACESSO NEGADO] Cargo " + f.cargo + " nao tem autoridade para: " + acao);
        registrarAuditoria(f.nome + " (" + f.ur + ")", "Acesso negado: " + acao);
        return false;
    }

    private static void painelFuncionario(Scanner sc, Funcionario func) {
        Cargo cargo = Cargo.deTexto(func.cargo);
        boolean ativo = true;

        while (ativo) {
            System.out.println("\n=================================================================");
            System.out.println("PAINEL INTERNO - " + func.nome + " [" + func.ur + " | " + cargo.name() + "]");
            System.out.println("=================================================================");
            if (cargo.pode(Permissao.GERIR_PESSOAS_DEMITIR_RECONTRATAR)) {
                System.out.println("1 - [RH] Equipe: cadastrar, demitir ou recontratar");
            }
            if (cargo.pode(Permissao.VER_E_AJUSTAR_SALARIOS)) {
                System.out.println("2 - [FINANCEIRO] Consultar/ajustar salarios");
            }
            if (cargo.pode(Permissao.VER_MOVIMENTACAO_E_HISTORICO_DINHEIRO)) {
                System.out.println("3 - [FINANCEIRO] Historico de movimentacoes e saldo de caixa");
            }
            if (cargo.pode(Permissao.MOVIMENTAR_ESTOQUE_E_PRODUTOS)) {
                System.out.println("4 - [ESTOQUE] Movimentar entrada/saida de produtos");
            }
            if (cargo.pode(Permissao.VER_FORNECEDORES_E_FORNECIMENTO)) {
                System.out.println("5 - [ESTOQUE] Fornecedores e o que cada um fornece");
            }
            if (cargo.pode(Permissao.REGISTRAR_VENDA_E_VER_REGISTRO_VENDAS)) {
                System.out.println("6 - [CAIXA] Ver registro de vendas");
            }
            if (cargo.pode(Permissao.VER_CLIENTES_CADASTRADOS)) {
                System.out.println("7 - Ver clientes cadastrados");
            }
            if (cargo.pode(Permissao.SUPERVISAO_GERAL)) {
                System.out.println("8 - [GESTAO] Resumo consolidado do sistema");
            }
            System.out.println("0 - Sair do painel");

            try {
                int op = lerOpcaoMenu(sc, "Opcao", 0, 8);
                switch (op) {
                    case 1:
                        if (autorizar(func, Permissao.GERIR_PESSOAS_DEMITIR_RECONTRATAR, "Gestao de RH")) {
                            menuRH(sc, func);
                        }
                        break;
                    case 2:
                        if (autorizar(func, Permissao.VER_E_AJUSTAR_SALARIOS, "Salarios Financeiro")) {
                            menuSalariosFinanceiro(sc, func);
                        }
                        break;
                    case 3:
                        if (autorizar(func, Permissao.VER_MOVIMENTACAO_E_HISTORICO_DINHEIRO, "Historico Financeiro")) {
                            System.out.println("\n--- HISTORICO FINANCEIRO (Saldo: " + formatarBRL(saldoCaixa) + ") ---");
                            for (MovimentoFinanceiro m : movimentosFinanceiros) {
                                System.out.printf("%s | %-14s | %-7s | %-22s | %11s | Saldo: %s%n",
                                        m.dataHora, m.tipo, m.origem, m.descricao, formatarBRL(m.valor), formatarBRL(m.saldoApos));
                            }
                        }
                        break;
                    case 4:
                        if (autorizar(func, Permissao.MOVIMENTAR_ESTOQUE_E_PRODUTOS, "Movimentar estoque")) {
                            movimentarEstoque(sc, func);
                        }
                        break;
                    case 5:
                        if (autorizar(func, Permissao.VER_FORNECEDORES_E_FORNECIMENTO, "Fornecedores")) {
                            System.out.println("\n--- FORNECEDORES E FORNECIMENTO ---");
                            for (Fornecedor fn : fornecedores) {
                                System.out.printf("%s | %-28s | CNPJ: %s | Fornece: %s%n",
                                        fn.codigo, fn.nome, fn.cnpj, fn.itensFornecidos);
                            }
                        }
                        break;
                    case 6:
                        if (autorizar(func, Permissao.REGISTRAR_VENDA_E_VER_REGISTRO_VENDAS, "Registro de vendas")) {
                            System.out.println("\n--- REGISTRO DE VENDAS ---");
                            for (RegistroVenda v : vendas) {
                                System.out.printf("NF #%d | %s | Cli: %s | Itens: %s | Imp: %s | Total: %s%n",
                                        v.numeroNF, v.dataHora, v.clienteUR, v.resumoItens, formatarBRL(v.impostos), formatarBRL(v.total));
                            }
                        }
                        break;
                    case 7:
                        if (autorizar(func, Permissao.VER_CLIENTES_CADASTRADOS, "Clientes cadastrados")) {
                            System.out.println("\n--- CLIENTES CADASTRADOS ---");
                            for (Cliente c : clientes) {
                                System.out.printf("%-8s | %-22s | CPF: %s | Bloqueado: %s%n",
                                        c.ur, c.nome, c.cpf, c.bloqueado);
                            }
                        }
                        break;
                    case 8:
                        if (autorizar(func, Permissao.SUPERVISAO_GERAL, "Supervisao Geral")) {
                            System.out.println("\n--- [GESTAO] RESUMO GERAL ---");
                            System.out.println("Funcionarios: " + funcionarios.size() + " | Clientes: " + clientes.size()
                                    + " | Produtos: " + produtos.size() + " | Fornecedores: " + fornecedores.size()
                                    + " | Vendas: " + vendas.size() + " | Caixa: " + formatarBRL(saldoCaixa));
                        }
                        break;
                    case 0:
                        ativo = false;
                        break;
                }
            } catch (RetornarTelaException ignored) {
                // Retorna ao inicio do painel do funcionario
            }
        }
    }

    private static void menuRH(Scanner sc, Funcionario executor) {
        System.out.println("\n--- [RH] EQUIPE ---");
        for (Funcionario f : funcionarios) {
            System.out.printf("%-8s | %-20s | %-10s | Status: %-8s | Bloq: %-5s%n",
                    f.ur, f.nome, f.cargo, f.statusRH, f.bloqueado);
        }
        String acao = lerDaLista(sc, "Acao RH", "CADASTRAR", "DEMITIR", "RECONTRATAR", "NENHUMA");
        if ("NENHUMA".equals(acao)) return;

        if ("CADASTRAR".equals(acao)) {
            String ur = lerURValida(sc, "UR do novo funcionario");
            for (Funcionario f : funcionarios) {
                if (f.ur.equalsIgnoreCase(ur)) {
                    System.out.println("[AVISO] UR ja existe.");
                    return;
                }
            }
            String nome = lerNomeSemNumeros(sc, "Nome completo");
            String cpf = lerCPFValido(sc);
            String cargo = lerDaLista(sc, "Cargo", "GESTAO", "RH", "FINANCEIRO", "ESTOQUE", "CAIXA");
            double sal = lerDecimalPositivo(sc, "Salario");
            String senha = lerSenhaValida(sc);
            Funcionario novo = new Funcionario(ur, nome, cpf, cargo, senha, sal, false, "ATIVO");
            funcionarios.add(novo);
            tabelaFuncSenha.put(novo.ur, novo.senha);
            registrarAuditoria(executor.nome, "RH cadastrou " + novo.ur + " (" + novo.cargo + ")");
            System.out.println("[OK] Funcionario cadastrado.");
            return;
        }

        String urAlvo = lerURValida(sc, "UR do funcionario");
        for (Funcionario f : funcionarios) {
            if (f.ur.equalsIgnoreCase(urAlvo)) {
                if ("DEMITIR".equals(acao)) {
                    if (f.ur.equalsIgnoreCase(executor.ur)) {
                        System.out.println("[AVISO] Nao pode demitir a si mesmo.");
                        return;
                    }
                    f.statusRH = "DEMITIDO";
                    f.bloqueado = true;
                    registrarAuditoria(executor.nome, "RH demitiu " + f.ur + " (" + f.nome + ")");
                    System.out.println("[OK] Funcionario demitido.");
                } else if ("RECONTRATAR".equals(acao)) {
                    f.statusRH = "ATIVO";
                    f.bloqueado = false;
                    f.tentativasFalhas = 0;
                    f.salario = lerDecimalPositivo(sc, "Novo salario de recontratacao");
                    registrarAuditoria(executor.nome, "RH recontratou " + f.ur + " (" + f.nome + ")");
                    System.out.println("[OK] Funcionario recontratado.");
                }
                return;
            }
        }
        System.out.println("[AVISO] Funcionario nao encontrado.");
    }

    private static void menuSalariosFinanceiro(Scanner sc, Funcionario executor) {
        System.out.println("\n--- [FINANCEIRO] SALARIOS ---");
        for (Funcionario f : funcionarios) {
            System.out.printf("%-8s | %-20s | %-10s | %-8s | Salario: %s%n",
                    f.ur, f.nome, f.cargo, f.statusRH, formatarBRL(f.salario));
        }
        String resp = lerDaLista(sc, "Ajustar salario?", "SIM", "NAO");
        if ("NAO".equals(resp)) return;

        String ur = lerURValida(sc, "UR do funcionario");
        for (Funcionario f : funcionarios) {
            if (f.ur.equalsIgnoreCase(ur)) {
                double novoSal = lerDecimalPositivo(sc, "Novo salario");
                f.salario = novoSal;
                movimentosFinanceiros.add(new MovimentoFinanceiro(
                        agoraTexto(), "AJUSTE_SALARIO", executor.ur, "Salario " + f.ur, novoSal, saldoCaixa
                ));
                registrarAuditoria(executor.nome, "Financeiro alterou salario de " + f.ur + " para " + novoSal);
                System.out.println("[OK] Salario atualizado.");
                return;
            }
        }
        System.out.println("[AVISO] Funcionario nao encontrado.");
    }

    private static void movimentarEstoque(Scanner sc, Funcionario executor) {
        listarProdutosComImposto();
        String cod = lerURValida(sc, "Codigo do produto");
        Produto p = buscarProdutoOficial(cod);
        if (p == null) {
            System.out.println("[AVISO] Produto nao encontrado.");
            return;
        }
        String tipo = lerDaLista(sc, "Tipo de movimento", "ENTRADA", "SAIDA");
        int qtd = lerInteiroPositivo(sc, "Quantidade");
        if ("ENTRADA".equals(tipo)) {
            p.estoque += qtd;
            movimentosFinanceiros.add(new MovimentoFinanceiro(
                    agoraTexto(), "ENTRADA", executor.ur, "+" + qtd + " " + p.codigo, 0.0, saldoCaixa
            ));
            System.out.println("[OK] Entrada registrada. Estoque atual: " + p.estoque);
        } else {
            if (qtd > p.estoque) {
                System.out.println("[AVISO] Estoque insuficiente (" + p.estoque + ").");
                return;
            }
            p.estoque -= qtd;
            movimentosFinanceiros.add(new MovimentoFinanceiro(
                    agoraTexto(), "SAIDA", executor.ur, "-" + qtd + " " + p.codigo, 0.0, saldoCaixa
            ));
            System.out.println("[OK] Saida registrada. Estoque atual: " + p.estoque);
        }
        registrarAuditoria(executor.nome, "Estoque " + tipo + " " + qtd + " em " + p.codigo);
    }

    // ==================== LOOP PRINCIPAL ====================
    public static void main(String[] args) {
        registrarShutdownHook();
        carregarDadosIniciais();

        try (Scanner sc = new Scanner(System.in)) {
            boolean rodando = true;
            while (rodando) {
                System.out.println("\n=======================================================");
                System.out.println("          SISTEMA DE GESTAO, ESTOQUE E VENDAS         ");
                System.out.println("=======================================================");
                System.out.println("1 - Login de Funcionario (GESTAO/RH/FINANCEIRO/ESTOQUE/CAIXA)");
                System.out.println("2 - Login de Cliente");
                System.out.println("3 - Cadastrar novo Cliente");
                System.out.println("0 - Sair (ou digite EMERGENCIA para saida e backup imediato)");

                System.out.print("Opcao (0 a 3, ou EMERGENCIA): ");
                String raw = sc.nextLine().trim();

                if ("EMERGENCIA".equalsIgnoreCase(raw)) {
                    executarSincronizacaoEBackupAoFechar(true, "SAIDA_EMERGENCIA");
                    rodando = false;
                    continue;
                }

                if (raw.startsWith("-") || raw.contains(".") || raw.contains(",") || !raw.matches("\\d+")) {
                    System.out.println("[AVISO] Digite apenas 0, 1, 2, 3 ou EMERGENCIA (sem '-0', decimais ou simbolos).");
                    continue;
                }

                int op = Integer.parseInt(raw);
                switch (op) {
                    case 1:
                        Funcionario func = loginFuncionario(sc);
                        if (func != null) painelFuncionario(sc, func);
                        break;
                    case 2:
                        Cliente cli = loginCliente(sc);
                        if (cli != null) painelCliente(sc, cli);
                        break;
                    case 3:
                        Cliente novo = cadastrarCliente(sc);
                        if (novo != null) painelCliente(sc, novo);
                        break;
                    case 0:
                        executarSincronizacaoEBackupAoFechar(true, "SAIDA_NORMAL");
                        rodando = false;
                        break;
                    default:
                        System.out.println("[AVISO] Opcao fora da lista (0 a 3).");
                        break;
                }
            }
        }
    }
}