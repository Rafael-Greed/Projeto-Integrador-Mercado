import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/**
 * Sistema Unificado de Gestao e Vendas (Projeto Academico - Sem Classes de Dominio).
 * Utiliza listas paralelas (ArrayList / List), Strings e metodos estaticos,
 * com tratamento centralizado de erros, validacao universal de entradas/senhas/arquivos
 * e protecao de indices em todas as operacoes.
 */
public class SistemaGestaoConceitual {

    // ==================== ARQUIVOS DE PERSISTENCIA E TESTE ====================
    private static final String ARQ_FUNCIONARIOS = "txt_funcionarios.txt";
    private static final String ARQ_CLIENTES = "txt_clientes.txt";
    private static final String ARQ_PRODUTOS = "txt_produtos.txt";
    private static final String ARQ_FORNECEDORES = "txt_fornecedores.txt";
    private static final String ARQ_HIST_ESTOQUE = "txt_hist_estoque.txt";
    private static final String ARQ_HIST_FINANCEIRO = "txt_hist_financeiro.txt";
    private static final String ARQ_HIST_ACESSOS = "txt_hist_acessos.txt";
    private static final String ARQ_HIST_VENDAS = "txt_hist_vendas.txt";
    private static final String ARQ_BACKUP = "backup_sistema.txt";
    private static final String ARQ_TESTE = "teste_cenario_sistema.txt";

    private static final List<String> ARQUIVOS_PERMITIDOS = Arrays.asList(
            ARQ_FUNCIONARIOS, ARQ_CLIENTES, ARQ_PRODUTOS, ARQ_FORNECEDORES,
            ARQ_HIST_ESTOQUE, ARQ_HIST_FINANCEIRO, ARQ_HIST_ACESSOS, ARQ_HIST_VENDAS,
            ARQ_BACKUP, ARQ_TESTE
    );

    private static final int ITENS_POR_PAGINA = 25;
    private static final int MAX_TENTATIVAS_LOGIN = 3;
    private static final double DEPOSITO_INICIAL_PADRAO = 150000.0;

    // Senha de confirmacao para alteracoes sensiveis envolvendo a Gerencia
    private static final String SENHA_CONFIRMACAO_GERENCIA = "GER123";

    private static final DateTimeFormatter FMT_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter FMT_DATA_NASC = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat FMT_MOEDA = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

    private static final List<String> UFS_VALIDAS = Arrays.asList(
            "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG",
            "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO"
    );

    // ==================== TABELAS PARALELAS: FUNCIONARIOS ====================
    private static final List<String> funcId = new ArrayList<>();
    private static final List<String> funcNome = new ArrayList<>();
    private static final List<String> funcCpf = new ArrayList<>();
    private static final List<String> funcDataNasc = new ArrayList<>(); // dd/MM/yyyy
    private static final List<String> funcSexo = new ArrayList<>();     // M / F / O
    private static final List<String> funcCargo = new ArrayList<>();    // GERENCIA, RH, FIN, EST, CAIXA, TI
    private static final List<String> funcSenha = new ArrayList<>();
    private static final List<Double> funcSalario = new ArrayList<>();
    private static final List<Boolean> funcPago = new ArrayList<>();
    private static final List<Boolean> funcAtivo = new ArrayList<>();
    private static final List<Boolean> funcBloqueado = new ArrayList<>();
    private static final List<Integer> funcTentativas = new ArrayList<>();
    private static final List<String> funcMotivoDemissao = new ArrayList<>();
    private static final List<String> funcAutoridades = new ArrayList<>();

    // ==================== TABELAS PARALELAS: CLIENTES ====================
    private static final List<String> cliId = new ArrayList<>();
    private static final List<String> cliCpf = new ArrayList<>();
    private static final List<String> cliNome = new ArrayList<>();

    // ==================== TABELAS PARALELAS: FORNECEDORES ====================
    private static final List<String> fornId = new ArrayList<>();
    private static final List<String> fornNome = new ArrayList<>();
    private static final List<String> fornCnpj = new ArrayList<>();
    private static final List<String> fornEstado = new ArrayList<>(); // UF
    private static final List<String> fornProdId = new ArrayList<>(); // Produto fornecido obrigatorio
    private static final List<Double> fornValorDevido = new ArrayList<>();

    // ==================== TABELAS PARALELAS: PRODUTOS ====================
    private static final List<String> prodId = new ArrayList<>();
    private static final List<String> prodNome = new ArrayList<>();
    private static final List<String> prodFornId = new ArrayList<>(); // Fornecedor obrigatorio
    private static final List<Double> prodCustoFornecedor = new ArrayList<>(); // Custo unitario pago ao fornecedor
    private static final List<Double> prodPreco = new ArrayList<>();           // Preco de venda ao cliente (> custo)
    private static final List<Integer> prodEstoque = new ArrayList<>();
    private static final List<Double> prodAliqAtual = new ArrayList<>();       // Aliquota deste ano (%)
    private static final List<Double> prodAliqProximo = new ArrayList<>();     // Aliquota do ano que vem (%)
    private static final List<Boolean> prodAtivo = new ArrayList<>();          // false quando excluido do catalogo

    // ==================== HISTORICOS EM LISTAS PARALELAS ====================
    private static final List<String> histEstData = new ArrayList<>();
    private static final List<String> histEstTipo = new ArrayList<>();
    private static final List<String> histEstProdId = new ArrayList<>();
    private static final List<String> histEstProdNome = new ArrayList<>();
    private static final List<Integer> histEstQtd = new ArrayList<>();
    private static final List<String> histEstDetalhe = new ArrayList<>();

    private static final List<String> histFinData = new ArrayList<>();
    private static final List<String> histFinTipo = new ArrayList<>();
    private static final List<String> histFinDescricao = new ArrayList<>();
    private static final List<Double> histFinValor = new ArrayList<>();
    private static final List<Double> histFinImposto = new ArrayList<>();
    private static final List<Double> histFinLucro = new ArrayList<>();
    private static final List<Double> histFinSaldoApos = new ArrayList<>();

    private static final List<String> histAcData = new ArrayList<>();
    private static final List<String> histAcUsuario = new ArrayList<>();
    private static final List<String> histAcAcao = new ArrayList<>();

    private static final List<Integer> vendaNfNum = new ArrayList<>();
    private static final List<String> vendaData = new ArrayList<>();
    private static final List<String> vendaCliCpf = new ArrayList<>();
    private static final List<String> vendaCliNome = new ArrayList<>();
    private static final List<String> vendaResumo = new ArrayList<>();
    private static final List<Double> vendaSubtotal = new ArrayList<>();
    private static final List<Double> vendaImposto = new ArrayList<>();
    private static final List<Double> vendaCustoTotal = new ArrayList<>();
    private static final List<Double> vendaLucroLiquido = new ArrayList<>();
    private static final List<Double> vendaTotal = new ArrayList<>();

    private static double saldoCaixa = DEPOSITO_INICIAL_PADRAO;
    private static int proximoNf = 1001;
    private static String usuarioLogadoId = null;
    private static boolean backupExecutadoAoSair = false;

    // ==================== MODULO CENTRAL DE ERROS E ACESSO SEGURO A INDICES ====================

    /**
     * Registra e exibe mensagens padronizadas do modulo de tratamento de erros.
     */
    private static void reportarErro(String categoria, String mensagem) {
        String cat = sanitizarTextoTxt(categoria);
        String msg = sanitizarTextoTxt(mensagem);
        System.out.println("[ERRO - " + (cat.isEmpty() ? "SISTEMA" : cat) + "] "
                + (msg.isEmpty() ? "Ocorreu uma falha inesperada." : msg));
    }

    /**
     * Acesso seguro a elemento String de lista por indice, com tratamento de excecao.
     */
    private static String obterStringSeguro(List<String> lista, int indice, String padrao) {
        try {
            if (lista == null) {
                throw new NullPointerException("Lista de textos nula.");
            }
            if (indice < 0 || indice >= lista.size()) {
                throw new IndexOutOfBoundsException("Indice " + indice + " fora do intervalo [0.." + (lista.size() - 1) + "].");
            }
            String val = lista.get(indice);
            return val != null ? val : padrao;
        } catch (IndexOutOfBoundsException | NullPointerException e) {
            reportarErro("ACESSO INDEVIDO A VETOR/INDICE", e.getMessage());
            return padrao;
        }
    }

    /**
     * Acesso seguro a elemento Double de lista por indice, com tratamento de excecao.
     */
    private static double obterDoubleSeguro(List<Double> lista, int indice, double padrao) {
        try {
            if (lista == null) {
                throw new NullPointerException("Lista numerica nula.");
            }
            if (indice < 0 || indice >= lista.size()) {
                throw new IndexOutOfBoundsException("Indice " + indice + " fora do intervalo [0.." + (lista.size() - 1) + "].");
            }
            Double val = lista.get(indice);
            if (val == null || Double.isNaN(val) || Double.isInfinite(val)) {
                return padrao;
            }
            return val;
        } catch (IndexOutOfBoundsException | NullPointerException e) {
            reportarErro("ACESSO INDEVIDO A VETOR/INDICE", e.getMessage());
            return padrao;
        }
    }

    /**
     * Acesso seguro a elemento Integer de lista por indice, com tratamento de excecao.
     */
    private static int obterIntSeguro(List<Integer> lista, int indice, int padrao) {
        try {
            if (lista == null) {
                throw new NullPointerException("Lista inteira nula.");
            }
            if (indice < 0 || indice >= lista.size()) {
                throw new IndexOutOfBoundsException("Indice " + indice + " fora do intervalo [0.." + (lista.size() - 1) + "].");
            }
            Integer val = lista.get(indice);
            return val != null ? val : padrao;
        } catch (IndexOutOfBoundsException | NullPointerException e) {
            reportarErro("ACESSO INDEVIDO A VETOR/INDICE", e.getMessage());
            return padrao;
        }
    }

    /**
     * Acesso seguro a elemento Boolean de lista por indice, com tratamento de excecao.
     */
    private static boolean obterBooleanSeguro(List<Boolean> lista, int indice, boolean padrao) {
        try {
            if (lista == null) {
                throw new NullPointerException("Lista booleana nula.");
            }
            if (indice < 0 || indice >= lista.size()) {
                throw new IndexOutOfBoundsException("Indice " + indice + " fora do intervalo [0.." + (lista.size() - 1) + "].");
            }
            Boolean val = lista.get(indice);
            return Boolean.TRUE.equals(val);
        } catch (IndexOutOfBoundsException | NullPointerException e) {
            reportarErro("ACESSO INDEVIDO A VETOR/INDICE", e.getMessage());
            return padrao;
        }
    }

    /**
     * Atualiza de forma segura um elemento String em uma lista.
     */
    private static boolean definirStringSeguro(List<String> lista, int indice, String valor) {
        try {
            if (lista == null || indice < 0 || indice >= lista.size()) {
                throw new IndexOutOfBoundsException("Falha ao atualizar texto no indice " + indice + ".");
            }
            lista.set(indice, sanitizarTextoTxt(valor));
            return true;
        } catch (IndexOutOfBoundsException | NullPointerException e) {
            reportarErro("ACESSO INDEVIDO A VETOR/INDICE", e.getMessage());
            return false;
        }
    }

    /**
     * Atualiza de forma segura um elemento Double em uma lista.
     */
    private static boolean definirDoubleSeguro(List<Double> lista, int indice, double valor) {
        try {
            if (lista == null || indice < 0 || indice >= lista.size()) {
                throw new IndexOutOfBoundsException("Falha ao atualizar decimal no indice " + indice + ".");
            }
            if (Double.isNaN(valor) || Double.isInfinite(valor)) {
                throw new IllegalArgumentException("Valor decimal invalido.");
            }
            lista.set(indice, valor);
            return true;
        } catch (IndexOutOfBoundsException | NullPointerException | IllegalArgumentException e) {
            reportarErro("ACESSO INDEVIDO A VETOR/INDICE", e.getMessage());
            return false;
        }
    }

    /**
     * Atualiza de forma segura um elemento Integer em uma lista.
     */
    private static boolean definirIntSeguro(List<Integer> lista, int indice, int valor) {
        try {
            if (lista == null || indice < 0 || indice >= lista.size()) {
                throw new IndexOutOfBoundsException("Falha ao atualizar inteiro no indice " + indice + ".");
            }
            lista.set(indice, valor);
            return true;
        } catch (IndexOutOfBoundsException | NullPointerException e) {
            reportarErro("ACESSO INDEVIDO A VETOR/INDICE", e.getMessage());
            return false;
        }
    }

    /**
     * Atualiza de forma segura um elemento Boolean em uma lista.
     */
    private static boolean definirBooleanSeguro(List<Boolean> lista, int indice, boolean valor) {
        try {
            if (lista == null || indice < 0 || indice >= lista.size()) {
                throw new IndexOutOfBoundsException("Falha ao atualizar booleano no indice " + indice + ".");
            }
            lista.set(indice, valor);
            return true;
        } catch (IndexOutOfBoundsException | NullPointerException e) {
            reportarErro("ACESSO INDEVIDO A VETOR/INDICE", e.getMessage());
            return false;
        }
    }

    // ==================== FUNCOES DE APOIO, CALCULOS E HISTORICOS ====================

    private static void exibirExplicacaoFuncoesEExcecoes() {
        System.out.println("\n=== EXPLICACAO DAS FUNCOES E TRATAMENTO DE EXCECOES ===");
        System.out.println("- Cliente: navega em paginas de 25 produtos, filtra/limpa filtro, gerencia carrinho e");
        System.out.println("  exige CPF e Nome (cadastrando ou usando existente) apenas ao finalizar a compra.");
        System.out.println("- Funcionario: acesso exige cadastro; submenus aparecem conforme autoridade valida.");
        System.out.println("- Gerencia: visualiza todo o sistema; exige ID e senha de autoridade.");
        System.out.println("- RH: contrata, cadastra, demite com motivo, readmite, edita dados (calculando idade");
        System.out.println("  pela data de nascimento) e confere se o funcionario ja foi pago.");
        System.out.println("- Estoque: lista itens, registra entradas/saidas, vincula produto e fornecedor, compra");
        System.out.println("  de fornecedores verificando saldo, exige preco de venda > custo, edita via modal e exclui mantendo historico.");
        System.out.println("- Financeiro: opera a partir do deposito inicial, paga salarios e fornecedores, ajusta");
        System.out.println("  salarios, define aliquotas deste ano e do proximo ano e detalha impostos e lucros.");
        System.out.println("- TI: desbloqueia login apos 3 erros, ajusta autoridades (exceto gerencia sem senha de");
        System.out.println("  confirmacao), consulta historico de acessos, cria backups e inspeciona .TXT.");
        System.out.println("- Excecoes tratadas: divisao por zero, entrada invalida (numero, data, senha,");
        System.out.println("  documento, arquivo) e acesso indevido a vetores/indices em todas as listas.");
    }

    private static int calcularIdade(String dataNascStr) {
        try {
            if (dataNascStr == null || dataNascStr.trim().isEmpty()) {
                throw new IllegalArgumentException("Data de nascimento vazia.");
            }
            LocalDate nasc = LocalDate.parse(dataNascStr.trim(), FMT_DATA_NASC);
            LocalDate hoje = LocalDate.now();
            if (nasc.isAfter(hoje)) {
                throw new IllegalArgumentException("Data de nascimento futura.");
            }
            return Period.between(nasc, hoje).getYears();
        } catch (DateTimeParseException | IllegalArgumentException e) {
            reportarErro("ENTRADA INVALIDA", "Data de nascimento invalida para calculo de idade: " + e.getMessage());
            return -1;
        }
    }

    private static double dividirComSeguranca(double dividendo, double divisor) {
        try {
            if (Double.isNaN(dividendo) || Double.isInfinite(dividendo) || Double.isNaN(divisor) || Double.isInfinite(divisor)) {
                throw new ArithmeticException("Operacao aritmetica com valor invalido.");
            }
            if (divisor == 0.0) {
                throw new ArithmeticException("Divisao por zero nao permitida.");
            }
            return dividendo / divisor;
        } catch (ArithmeticException e) {
            reportarErro("DIVISAO POR ZERO", e.getMessage() + " Retornando 0.00.");
            return 0.0;
        }
    }

    private static double calcularImpostoUnitario(double precoVenda, double aliquotaPercent) {
        if (precoVenda < 0.0 || aliquotaPercent < 0.0) {
            reportarErro("ENTRADA INVALIDA", "Valores negativos nao permitidos no calculo de imposto.");
            return 0.0;
        }
        return precoVenda * (aliquotaPercent / 100.0);
    }

    private static double calcularLucroLiquidoUnitario(double precoVenda, double custoFornecedor, double aliquotaPercent) {
        double impUnit = calcularImpostoUnitario(precoVenda, aliquotaPercent);
        return precoVenda - custoFornecedor - impUnit;
    }

    private static String sanitizarTextoTxt(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace(";", ",").replace("\r", " ").replace("\n", " ").trim();
    }

    private static void registrarAcesso(String usuario, String acao) {
        String data = LocalDateTime.now().format(FMT_DATA_HORA);
        histAcData.add(data);
        histAcUsuario.add(sanitizarTextoTxt(usuario));
        histAcAcao.add(sanitizarTextoTxt(acao));
        salvarHistoricoAcessosTxt();
    }

    private static void registrarHistEstoque(String tipo, String pId, String pNome, int qtd, String detalhe) {
        histEstData.add(LocalDateTime.now().format(FMT_DATA_HORA));
        histEstTipo.add(sanitizarTextoTxt(tipo));
        histEstProdId.add(sanitizarTextoTxt(pId));
        histEstProdNome.add(sanitizarTextoTxt(pNome));
        histEstQtd.add(qtd);
        histEstDetalhe.add(sanitizarTextoTxt(detalhe));
        salvarHistoricoEstoqueTxt();
    }

    private static void registrarHistFinanceiro(String tipo, String descricao, double valor,
                                                double imposto, double lucro, double saldoAtual) {
        histFinData.add(LocalDateTime.now().format(FMT_DATA_HORA));
        histFinTipo.add(sanitizarTextoTxt(tipo));
        histFinDescricao.add(sanitizarTextoTxt(descricao));
        histFinValor.add(valor);
        histFinImposto.add(imposto);
        histFinLucro.add(lucro);
        histFinSaldoApos.add(saldoAtual);
        salvarHistoricoFinanceiroTxt();
    }

    // ==================== VALIDACAO UNIVERSAL DE ENTRADAS, SENHAS E ARQUIVOS ====================

    private static boolean ehComandoVoltar(String entrada) {
        if (entrada == null) {
            return false;
        }
        String limpo = entrada.trim().toUpperCase();
        return limpo.equals("0") || limpo.equals("VOLTAR") || limpo.equals("CANCELAR");
    }

    /**
     * Leitura segura de linha do Scanner com tratamento de excecao.
     */
    private static String lerLinhaSegura(Scanner sc) {
        try {
            if (sc == null || !sc.hasNextLine()) {
                return null;
            }
            return sc.nextLine();
        } catch (IllegalStateException e) {
            reportarErro("ENTRADA INVALIDA", "Fluxo de entrada indisponivel.");
            return null;
        }
    }

    private static String lerTextoObrigatorio(Scanner sc, String rotulo) {
        while (true) {
            try {
                System.out.print(rotulo + " [0/VOLTAR para cancelar]: ");
                String linha = lerLinhaSegura(sc);
                if (linha == null || ehComandoVoltar(linha)) {
                    return null;
                }
                String limpo = sanitizarTextoTxt(linha);
                if (limpo.isEmpty()) {
                    throw new IllegalArgumentException("O campo '" + rotulo + "' nao pode ficar vazio.");
                }
                return limpo;
            } catch (IllegalArgumentException e) {
                reportarErro("ENTRADA INVALIDA", e.getMessage());
            }
        }
    }

    /**
     * Valida e le senhas (inicial, nova ou de confirmacao), impedindo espacos/vazios
     * e exigindo tamanho minimo de 4 caracteres.
     */
    private static String lerSenhaValida(Scanner sc, String rotulo, boolean exigirMinimo) {
        while (true) {
            try {
                System.out.print(rotulo + " [0/VOLTAR para cancelar]: ");
                String linha = lerLinhaSegura(sc);
                if (linha == null || ehComandoVoltar(linha)) {
                    return null;
                }
                String senha = linha.trim();
                if (senha.isEmpty()) {
                    throw new IllegalArgumentException("A senha nao pode ficar em branco.");
                }
                if (senha.contains(";")) {
                    throw new IllegalArgumentException("A senha nao pode conter ';'.");
                }
                if (exigirMinimo && senha.length() < 4) {
                    throw new IllegalArgumentException("A senha deve ter pelo menos 4 caracteres.");
                }
                return senha;
            } catch (IllegalArgumentException e) {
                reportarErro("SENHA INVALIDA", e.getMessage());
            }
        }
    }

    /**
     * Valida e le nome de arquivo .TXT permitido, bloqueando path traversal (../, / ou \).
     */
    private static String lerNomeArquivoTxtValido(Scanner sc, String rotulo) {
        while (true) {
            try {
                String bruto = lerTextoObrigatorio(sc, rotulo);
                if (bruto == null) {
                    return null;
                }
                String nome = bruto.trim();
                if (nome.contains("..") || nome.contains("/") || nome.contains("\\")) {
                    throw new IllegalArgumentException("Caminho de arquivo nao permitido.");
                }
                if (!nome.toLowerCase().endsWith(".txt")) {
                    throw new IllegalArgumentException("Informe um arquivo com extensao .txt.");
                }
                Path p = Paths.get(nome).getFileName();
                if (p == null) {
                    throw new IllegalArgumentException("Nome de arquivo invalido.");
                }
                String nomeLimpo = p.toString();
                if (!ARQUIVOS_PERMITIDOS.contains(nomeLimpo)) {
                    throw new IllegalArgumentException("Arquivo fora da lista de arquivos do sistema. Permitidos: "
                            + String.join(", ", ARQUIVOS_PERMITIDOS));
                }
                return nomeLimpo;
            } catch (IllegalArgumentException e) {
                reportarErro("ARQUIVO INVALIDO", e.getMessage());
            }
        }
    }

    private static int lerInteiroFaixa(Scanner sc, String rotulo, int min, int max, boolean zeroCancela) {
        while (true) {
            try {
                if (min > max) {
                    throw new IllegalArgumentException("Faixa numerica invalida (" + min + " a " + max + ").");
                }
                System.out.print(rotulo + " (" + min + " a " + max + ") [VOLTAR para cancelar]: ");
                String raw = lerLinhaSegura(sc);
                if (raw == null) {
                    return Integer.MIN_VALUE;
                }
                String linha = raw.trim();
                if (linha.equalsIgnoreCase("VOLTAR") || linha.equalsIgnoreCase("CANCELAR")
                        || (zeroCancela && linha.equals("0"))) {
                    return Integer.MIN_VALUE;
                }
                int valor = Integer.parseInt(linha);
                if (valor < min || valor > max) {
                    throw new IllegalArgumentException("Valor fora da faixa permitida (" + min + " a " + max + ").");
                }
                return valor;
            } catch (NumberFormatException e) {
                reportarErro("ENTRADA INVALIDA", "Digite apenas um numero inteiro valido.");
            } catch (IllegalArgumentException e) {
                reportarErro("ENTRADA INVALIDA", e.getMessage());
            }
        }
    }

    private static double lerDecimalMinimo(Scanner sc, String rotulo, double min) {
        while (true) {
            try {
                System.out.print(rotulo + " [0/VOLTAR para cancelar]: ");
                String raw = lerLinhaSegura(sc);
                if (raw == null) {
                    return -1.0;
                }
                String linha = raw.trim().replace(",", ".");
                if (ehComandoVoltar(linha)) {
                    return -1.0;
                }
                double valor = Double.parseDouble(linha);
                if (Double.isNaN(valor) || Double.isInfinite(valor) || valor < min) {
                    throw new IllegalArgumentException("Informe um valor numerico valido (>= " + min + ").");
                }
                return valor;
            } catch (NumberFormatException e) {
                reportarErro("ENTRADA INVALIDA", "Digite um numero decimal valido.");
            } catch (IllegalArgumentException e) {
                reportarErro("ENTRADA INVALIDA", e.getMessage());
            }
        }
    }

    private static double lerPrecoVendaComLucro(Scanner sc, double custoFornecedor, double aliquotaAtual) {
        while (true) {
            System.out.println("\n--- DEFINICAO DE PRECO DE VENDA (COM LUCRO) ---");
            System.out.println("Custo unitario fornecedor : " + FMT_MOEDA.format(custoFornecedor));
            System.out.printf("Aliquota imposto vigente  : %.2f%%%n", aliquotaAtual);
            System.out.println("1 - Definir por margem percentual (%) sobre o custo");
            System.out.println("2 - Informar preco de venda diretamente");
            int modo = lerInteiroFaixa(sc, "Escolha o modo de precificacao", 1, 2, true);
            if (modo == Integer.MIN_VALUE) {
                return -1.0;
            }

            double precoCandidato;
            if (modo == 1) {
                double margem = lerDecimalMinimo(sc, "Margem desejada (%) [deve ser > 0]", 0.01);
                if (margem < 0) {
                    return -1.0;
                }
                precoCandidato = Math.round((custoFornecedor * (1.0 + margem / 100.0)) * 100.0) / 100.0;
            } else {
                precoCandidato = lerDecimalMinimo(sc, "Preco de venda ao cliente (deve ser > "
                        + FMT_MOEDA.format(custoFornecedor) + ")", 0.01);
                if (precoCandidato < 0) {
                    return -1.0;
                }
            }

            if (precoCandidato <= custoFornecedor) {
                reportarErro("PRECO INVALIDO", "O preco de venda (" + FMT_MOEDA.format(precoCandidato)
                        + ") NAO pode ser menor ou igual ao custo do fornecedor ("
                        + FMT_MOEDA.format(custoFornecedor) + ").");
                continue;
            }

            double impUnit = calcularImpostoUnitario(precoCandidato, aliquotaAtual);
            double lucroBrutoUnit = precoCandidato - custoFornecedor;
            double lucroLiqUnit = calcularLucroLiquidoUnitario(precoCandidato, custoFornecedor, aliquotaAtual);

            System.out.println("Preco de venda definido   : " + FMT_MOEDA.format(precoCandidato));
            System.out.println("Diferenca bruta p/ custo  : " + FMT_MOEDA.format(lucroBrutoUnit));
            System.out.println("Imposto unitario (" + String.format("%.1f", aliquotaAtual) + "%)   : " + FMT_MOEDA.format(impUnit));
            System.out.println("Lucro liquido estimado/un : " + FMT_MOEDA.format(lucroLiqUnit));

            if (lucroLiqUnit <= 0.0) {
                System.out.println("[ALERTA FISCAL] O preco e maior que o custo, mas o imposto vigente consome o lucro!");
                System.out.println("1 - Manter assim mesmo | 2 - Reajustar preco para cobrir imposto e dar lucro real");
                int resp = lerInteiroFaixa(sc, "Escolha", 1, 2, true);
                if (resp == Integer.MIN_VALUE) {
                    return -1.0;
                }
                if (resp == 2) {
                    continue;
                }
            }
            return precoCandidato;
        }
    }

    private static boolean validarCpf(String cpfRaw) {
        if (cpfRaw == null) {
            return false;
        }
        String cpf = cpfRaw.replaceAll("\\D", "");
        if (!cpf.matches("\\d{11}") || cpf.chars().distinct().count() == 1) {
            return false;
        }
        try {
            int soma1 = 0;
            int soma2 = 0;
            for (int i = 0; i < 9; i++) {
                int d = cpf.charAt(i) - '0';
                soma1 += d * (10 - i);
                soma2 += d * (11 - i);
            }
            int dv1 = (soma1 * 10) % 11;
            if (dv1 == 10) dv1 = 0;
            soma2 += dv1 * 2;
            int dv2 = (soma2 * 10) % 11;
            if (dv2 == 10) dv2 = 0;
            return dv1 == (cpf.charAt(9) - '0') && dv2 == (cpf.charAt(10) - '0');
        } catch (IndexOutOfBoundsException e) {
            reportarErro("ACESSO INDEVIDO A VETOR/INDICE", "Falha ao ler digitos do CPF.");
            return false;
        }
    }

    private static String lerCpfValido(Scanner sc) {
        while (true) {
            String entrada = lerTextoObrigatorio(sc, "CPF (11 digitos)");
            if (entrada == null) {
                return null;
            }
            String digitos = entrada.replaceAll("\\D", "");
            if (validarCpf(digitos)) {
                return digitos;
            }
            reportarErro("CPF INVALIDO", "CPF incorreto ou digitos verificadores invalidos.");
        }
    }

    private static boolean validarCnpj(String cnpjRaw) {
        if (cnpjRaw == null) {
            return false;
        }
        String cnpj = cnpjRaw.replaceAll("\\D", "");
        if (!cnpj.matches("\\d{14}") || cnpj.chars().distinct().count() == 1) {
            return false;
        }
        int[] peso1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] peso2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        try {
            int soma1 = 0;
            for (int i = 0; i < 12; i++) {
                soma1 += (cnpj.charAt(i) - '0') * peso1[i];
            }
            int dv1 = soma1 % 11;
            dv1 = (dv1 < 2) ? 0 : (11 - dv1);

            int soma2 = 0;
            for (int i = 0; i < 13; i++) {
                int d = (i == 12) ? dv1 : (cnpj.charAt(i) - '0');
                soma2 += d * peso2[i];
            }
            int dv2 = soma2 % 11;
            dv2 = (dv2 < 2) ? 0 : (11 - dv2);

            return dv1 == (cnpj.charAt(12) - '0') && dv2 == (cnpj.charAt(13) - '0');
        } catch (IndexOutOfBoundsException e) {
            reportarErro("ACESSO INDEVIDO A VETOR/INDICE", "Falha ao ler digitos do CNPJ.");
            return false;
        }
    }

    private static String lerCnpjValido(Scanner sc) {
        while (true) {
            String entrada = lerTextoObrigatorio(sc, "CNPJ (14 digitos)");
            if (entrada == null) {
                return null;
            }
            String digitos = entrada.replaceAll("\\D", "");
            if (validarCnpj(digitos)) {
                return digitos;
            }
            reportarErro("CNPJ INVALIDO", "CNPJ incorreto ou digitos verificadores invalidos.");
        }
    }

    private static String lerIdValido(Scanner sc, String rotulo) {
        while (true) {
            String id = lerTextoObrigatorio(sc, rotulo);
            if (id == null) {
                return null;
            }
            String up = id.toUpperCase();
            if (up.matches("[A-Z0-9_-]{3,20}")) {
                return up;
            }
            reportarErro("ID/USUARIO INVALIDO", "Use 3 a 20 caracteres (letras, numeros, '-' ou '_').");
        }
    }

    private static String lerUfValida(Scanner sc) {
        while (true) {
            String uf = lerTextoObrigatorio(sc, "Estado (UF - ex: SP, RJ, MG)");
            if (uf == null) {
                return null;
            }
            String up = uf.toUpperCase();
            if (UFS_VALIDAS.contains(up)) {
                return up;
            }
            reportarErro("UF INVALIDA", "Informe uma sigla de estado brasileira valida.");
        }
    }

    private static String lerDataNascimentoValida(Scanner sc) {
        while (true) {
            String data = lerTextoObrigatorio(sc, "Data de Nascimento (dd/MM/yyyy)");
            if (data == null) {
                return null;
            }
            if (calcularIdade(data) >= 0) {
                return data;
            }
        }
    }

    private static String lerSexoValido(Scanner sc) {
        while (true) {
            String sx = lerTextoObrigatorio(sc, "Sexo (M / F / O)");
            if (sx == null) {
                return null;
            }
            String up = sx.toUpperCase();
            if (up.equals("M") || up.equals("F") || up.equals("O")) {
                return up;
            }
            reportarErro("SEXO INVALIDO", "Digite M, F ou O.");
        }
    }

    // ==================== PERMISSOES E VALIDACAO DE SESSAO ====================

    private static boolean funcionarioEstaValido(int idx) {
        return idx >= 0 && idx < funcId.size()
                && obterBooleanSeguro(funcAtivo, idx, false)
                && !obterBooleanSeguro(funcBloqueado, idx, true);
    }

    private static boolean possuiAutoridade(String idFunc, String autoridadeNecessaria) {
        if (idFunc == null) {
            return false;
        }
        int idx = funcId.indexOf(idFunc);
        if (!funcionarioEstaValido(idx)) {
            return false;
        }
        if ("GERENCIA".equalsIgnoreCase(obterStringSeguro(funcCargo, idx, ""))) {
            return true;
        }
        String listaAut = obterStringSeguro(funcAutoridades, idx, "");
        if (listaAut.trim().isEmpty()) {
            return false;
        }
        List<String> partes = Arrays.asList(listaAut.toUpperCase().split(","));
        return partes.contains(autoridadeNecessaria.toUpperCase()) || partes.contains("GERENCIA");
    }

    private static boolean exigirAutoridade(String autoridadeNecessaria, String contexto) {
        if (usuarioLogadoId == null) {
            reportarErro("ACESSO NEGADO", "Nenhum funcionario logado.");
            return false;
        }
        int idx = funcId.indexOf(usuarioLogadoId);
        if (idx == -1 || !obterBooleanSeguro(funcAtivo, idx, false)) {
            reportarErro("ACESSO NEGADO", "Funcionario desativado ou inexistente: autoridade invalida.");
            return false;
        }
        if (obterBooleanSeguro(funcBloqueado, idx, false)) {
            reportarErro("ACESSO NEGADO", "Conta bloqueada. Procure o setor de TI.");
            return false;
        }
        if (!possuiAutoridade(usuarioLogadoId, autoridadeNecessaria)) {
            reportarErro("ACESSO NEGADO", "Voce nao possui autoridade para: " + contexto);
            registrarAcesso(usuarioLogadoId, "Bloqueado em: " + contexto);
            return false;
        }
        return true;
    }

    private static boolean confirmarSenhaGerencia(Scanner sc) {
        String senha = lerSenhaValida(sc, "Senha de confirmacao da Gerencia", false);
        if (senha == null) {
            System.out.println("[CANCELADO] Confirmacao da Gerencia cancelada.");
            return false;
        }
        if (!senha.equals(SENHA_CONFIRMACAO_GERENCIA)) {
            reportarErro("SENHA INVALIDA", "Senha de confirmacao da Gerencia incorreta.");
            registrarAcesso(usuarioLogadoId != null ? usuarioLogadoId : "DESCONHECIDO",
                    "Falha na senha de confirmacao da Gerencia");
            return false;
        }
        return true;
    }

    // ==================== BASE PADRAO, ARQUIVO DE TESTE E QUARENTENA ====================

    private static void carregarBasePadrao() {
        funcId.clear(); funcNome.clear(); funcCpf.clear(); funcDataNasc.clear();
        funcSexo.clear(); funcCargo.clear(); funcSenha.clear(); funcSalario.clear();
        funcPago.clear(); funcAtivo.clear(); funcBloqueado.clear(); funcTentativas.clear();
        funcMotivoDemissao.clear(); funcAutoridades.clear();

        cliId.clear(); cliCpf.clear(); cliNome.clear();
        fornId.clear(); fornNome.clear(); fornCnpj.clear(); fornEstado.clear();
        fornProdId.clear(); fornValorDevido.clear();

        prodId.clear(); prodNome.clear(); prodFornId.clear(); prodCustoFornecedor.clear();
        prodPreco.clear(); prodEstoque.clear(); prodAliqAtual.clear();
        prodAliqProximo.clear(); prodAtivo.clear();

        histEstData.clear(); histEstTipo.clear(); histEstProdId.clear();
        histEstProdNome.clear(); histEstQtd.clear(); histEstDetalhe.clear();

        histFinData.clear(); histFinTipo.clear(); histFinDescricao.clear();
        histFinValor.clear(); histFinImposto.clear(); histFinLucro.clear(); histFinSaldoApos.clear();

        histAcData.clear(); histAcUsuario.clear(); histAcAcao.clear();

        vendaNfNum.clear(); vendaData.clear(); vendaCliCpf.clear(); vendaCliNome.clear();
        vendaResumo.clear(); vendaSubtotal.clear(); vendaImposto.clear();
        vendaCustoTotal.clear(); vendaLucroLiquido.clear(); vendaTotal.clear();

        saldoCaixa = DEPOSITO_INICIAL_PADRAO;
        proximoNf = 1001;

        funcId.add("UR-GER01"); funcNome.add("MARIANA SOUZA"); funcCpf.add("11144477735");
        funcDataNasc.add("15/04/1988"); funcSexo.add("F"); funcCargo.add("GERENCIA");
        funcSenha.add("ger123"); funcSalario.add(12000.0); funcPago.add(false);
        funcAtivo.add(true); funcBloqueado.add(false); funcTentativas.add(0);
        funcMotivoDemissao.add("N/A"); funcAutoridades.add("GERENCIA");

        funcId.add("UR-RH01"); funcNome.add("CARLOS EDUARDO LIMA"); funcCpf.add("33366699917");
        funcDataNasc.add("22/09/1994"); funcSexo.add("M"); funcCargo.add("RH");
        funcSenha.add("rh123"); funcSalario.add(6200.0); funcPago.add(false);
        funcAtivo.add(true); funcBloqueado.add(false); funcTentativas.add(0);
        funcMotivoDemissao.add("N/A"); funcAutoridades.add("RH");

        funcId.add("UR-FIN01"); funcNome.add("BEATRIZ ALMEIDA"); funcCpf.add("22255588809");
        funcDataNasc.add("10/01/1991"); funcSexo.add("F"); funcCargo.add("FIN");
        funcSenha.add("fin123"); funcSalario.add(6800.0); funcPago.add(false);
        funcAtivo.add(true); funcBloqueado.add(false); funcTentativas.add(0);
        funcMotivoDemissao.add("N/A"); funcAutoridades.add("FINANCEIRO");

        funcId.add("UR-EST01"); funcNome.add("RAFAEL SANTOS"); funcCpf.add("44477711125");
        funcDataNasc.add("05/07/1997"); funcSexo.add("M"); funcCargo.add("EST");
        funcSenha.add("est123"); funcSalario.add(4900.0); funcPago.add(false);
        funcAtivo.add(true); funcBloqueado.add(false); funcTentativas.add(0);
        funcMotivoDemissao.add("N/A"); funcAutoridades.add("ESTOQUE");

        funcId.add("UR-CAIXA01"); funcNome.add("JULIANA ROCHA"); funcCpf.add("55588822233");
        funcDataNasc.add("19/11/1999"); funcSexo.add("F"); funcCargo.add("CAIXA");
        funcSenha.add("cx123"); funcSalario.add(3900.0); funcPago.add(false);
        funcAtivo.add(true); funcBloqueado.add(false); funcTentativas.add(0);
        funcMotivoDemissao.add("N/A"); funcAutoridades.add("CAIXA");

        funcId.add("UR-TI01"); funcNome.add("LUCAS MENDES"); funcCpf.add("77788899902");
        funcDataNasc.add("30/03/1995"); funcSexo.add("M"); funcCargo.add("TI");
        funcSenha.add("ti123"); funcSalario.add(7100.0); funcPago.add(false);
        funcAtivo.add(true); funcBloqueado.add(false); funcTentativas.add(0);
        funcMotivoDemissao.add("N/A"); funcAutoridades.add("TI");

        cliId.add("CLI-001"); cliCpf.add("66699933341"); cliNome.add("ANA CLARA OLIVEIRA");

        fornId.add("FOR-001"); fornNome.add("DISTRIBUIDORA NACIONAL LTDA");
        fornCnpj.add("11222333000181"); fornEstado.add("SP");
        fornProdId.add("PRD-001"); fornValorDevido.add(0.0);

        prodId.add("PRD-001"); prodNome.add("CADERNO UNIVERSITARIO 200 FLS");
        prodFornId.add("FOR-001"); prodCustoFornecedor.add(14.0); prodPreco.add(25.0);
        prodEstoque.add(50); prodAliqAtual.add(12.0); prodAliqProximo.add(14.0); prodAtivo.add(true);

        registrarHistFinanceiro("DEPOSITO_INICIAL", "Deposito inicial de operacoes do sistema",
                DEPOSITO_INICIAL_PADRAO, 0.0, 0.0, saldoCaixa);
        registrarHistEstoque("CADASTRO_INICIAL", "PRD-001", "CADERNO UNIVERSITARIO 200 FLS",
                50, "Estoque inicial (Custo: R$ 14,00 | Venda: R$ 25,00)");
    }

    private static void criarArquivoTesteSeNaoExistir() {
        File f = new File(ARQ_TESTE);
        if (f.exists()) {
            return;
        }
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(f))) {
            bw.write("# ARQUIVO TXT DE TESTE - SISTEMA UNIFICADO\n");
            bw.write("# Exemplo de produto valido (precoVenda > custoFornecedor): PRD-001;CADERNO;FOR-001;14.0;25.0;50;12.0;14.0;true\n");
            bw.write("# Exemplo de venda valida: 1001;25/11/2026 10:00:00;66699933341;ANA CLARA;1x PRD-001;25.0;3.0;14.0;8.0;28.0\n");
            bw.write("# Para testar quarentena: copie uma linha com precoVenda <= custoFornecedor ou produto inexistente para backup_sistema.txt\n");
        } catch (IOException e) {
            reportarErro("ARQUIVO", "Nao foi possivel criar " + ARQ_TESTE + ": " + e.getMessage());
        }
    }

    private static boolean verificarConsistenciaSistema() {
        int nProd = prodId.size();
        if (prodNome.size() != nProd || prodFornId.size() != nProd || prodCustoFornecedor.size() != nProd
                || prodPreco.size() != nProd || prodEstoque.size() != nProd || prodAliqAtual.size() != nProd
                || prodAliqProximo.size() != nProd || prodAtivo.size() != nProd) {
            return false;
        }
        for (int i = 0; i < nProd; i++) {
            double custo = obterDoubleSeguro(prodCustoFornecedor, i, -1.0);
            double preco = obterDoubleSeguro(prodPreco, i, -1.0);
            int est = obterIntSeguro(prodEstoque, i, -1);
            String fRef = obterStringSeguro(prodFornId, i, "");
            if (est < 0 || custo < 0.0 || preco <= custo || !fornId.contains(fRef)) {
                return false;
            }
        }

        int nForn = fornId.size();
        if (fornNome.size() != nForn || fornCnpj.size() != nForn || fornEstado.size() != nForn
                || fornProdId.size() != nForn || fornValorDevido.size() != nForn) {
            return false;
        }
        for (int i = 0; i < nForn; i++) {
            String uf = obterStringSeguro(fornEstado, i, "");
            String pRef = obterStringSeguro(fornProdId, i, "");
            if (!UFS_VALIDAS.contains(uf) || !prodId.contains(pRef)) {
                return false;
            }
        }

        for (int i = 0; i < vendaNfNum.size(); i++) {
            double sub = obterDoubleSeguro(vendaSubtotal, i, -1.0);
            double imp = obterDoubleSeguro(vendaImposto, i, -1.0);
            double tot = obterDoubleSeguro(vendaTotal, i, -1.0);
            if (sub < 0 || imp < 0 || tot < 0 || Math.abs((sub + imp) - tot) > 0.05) {
                return false;
            }
            String resumo = obterStringSeguro(vendaResumo, i, "");
            if (resumo.trim().isEmpty()) {
                return false;
            }
            String[] partes = resumo.split(",");
            for (String parte : partes) {
                String[] tokens = parte.trim().split("\\s+");
                if (tokens.length >= 2 && !prodId.contains(tokens[1].trim())) {
                    return false;
                }
            }
        }

        if (!histFinSaldoApos.isEmpty()) {
            double ultimoSaldoHist = obterDoubleSeguro(histFinSaldoApos, histFinSaldoApos.size() - 1, saldoCaixa);
            if (Math.abs(saldoCaixa - ultimoSaldoHist) > 0.05) {
                return false;
            }
        }
        return true;
    }

    private static void colocarArquivosEmQuarentena() {
        String carimbo = "QUARENTENA_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + "_";
        String[] candidatos = {
                ARQ_BACKUP, ARQ_FUNCIONARIOS, ARQ_CLIENTES, ARQ_PRODUTOS,
                ARQ_FORNECEDORES, ARQ_HIST_ESTOQUE, ARQ_HIST_FINANCEIRO, ARQ_HIST_VENDAS
        };
        for (String nome : candidatos) {
            try {
                File orig = new File(nome);
                if (orig.exists()) {
                    File dest = new File(carimbo + nome);
                    boolean renomeou = orig.renameTo(dest);
                    System.out.println("  -> Arquivo em quarentena: " + nome
                            + (renomeou ? " -> " + dest.getName() : " (falha ao renomear)"));
                }
            } catch (SecurityException e) {
                reportarErro("ARQUIVO", "Sem permissao para mover " + nome + " para quarentena.");
            }
        }
    }

    private static boolean lerArquivoBackup(File arq) {
        carregarBasePadrao();
        funcId.clear(); funcNome.clear(); funcCpf.clear(); funcDataNasc.clear();
        funcSexo.clear(); funcCargo.clear(); funcSenha.clear(); funcSalario.clear();
        funcPago.clear(); funcAtivo.clear(); funcBloqueado.clear(); funcTentativas.clear();
        funcMotivoDemissao.clear(); funcAutoridades.clear();
        cliId.clear(); cliCpf.clear(); cliNome.clear();
        fornId.clear(); fornNome.clear(); fornCnpj.clear(); fornEstado.clear();
        fornProdId.clear(); fornValorDevido.clear();
        prodId.clear(); prodNome.clear(); prodFornId.clear(); prodCustoFornecedor.clear();
        prodPreco.clear(); prodEstoque.clear(); prodAliqAtual.clear();
        prodAliqProximo.clear(); prodAtivo.clear();
        histFinData.clear(); histFinTipo.clear(); histFinDescricao.clear();
        histFinValor.clear(); histFinImposto.clear(); histFinLucro.clear(); histFinSaldoApos.clear();
        vendaNfNum.clear(); vendaData.clear(); vendaCliCpf.clear(); vendaCliNome.clear();
        vendaResumo.clear(); vendaSubtotal.clear(); vendaImposto.clear();
        vendaCustoTotal.clear(); vendaLucroLiquido.clear(); vendaTotal.clear();

        String secao = "";
        try (BufferedReader br = new BufferedReader(new FileReader(arq))) {
            String linha;
            while ((linha = br.readLine()) != null) {
                linha = linha.trim();
                if (linha.isEmpty() || linha.startsWith("#")) {
                    continue;
                }
                if (linha.startsWith("[") && linha.endsWith("]")) {
                    secao = linha.substring(1, linha.length() - 1);
                    continue;
                }
                String[] c = linha.split(";");
                if ("CONTROLE".equals(secao)) {
                    if (linha.startsWith("SALDO=")) {
                        saldoCaixa = Double.parseDouble(linha.substring(6).trim());
                    } else if (linha.startsWith("PROX_NF=")) {
                        proximoNf = Integer.parseInt(linha.substring(8).trim());
                    }
                } else if ("FUNCIONARIOS".equals(secao) && c.length >= 14) {
                    funcId.add(c[0]); funcNome.add(c[1]); funcCpf.add(c[2]); funcDataNasc.add(c[3]);
                    funcSexo.add(c[4]); funcCargo.add(c[5]); funcSenha.add(c[6]);
                    funcSalario.add(Double.parseDouble(c[7])); funcPago.add(Boolean.parseBoolean(c[8]));
                    funcAtivo.add(Boolean.parseBoolean(c[9])); funcBloqueado.add(Boolean.parseBoolean(c[10]));
                    funcTentativas.add(Integer.parseInt(c[11])); funcMotivoDemissao.add(c[12]);
                    funcAutoridades.add(c[13]);
                } else if ("CLIENTES".equals(secao) && c.length >= 3) {
                    cliId.add(c[0]); cliCpf.add(c[1]); cliNome.add(c[2]);
                } else if ("FORNECEDORES".equals(secao) && c.length >= 6) {
                    fornId.add(c[0]); fornNome.add(c[1]); fornCnpj.add(c[2]); fornEstado.add(c[3]);
                    fornProdId.add(c[4]); fornValorDevido.add(Double.parseDouble(c[5]));
                } else if ("PRODUTOS".equals(secao) && c.length >= 9) {
                    prodId.add(c[0]); prodNome.add(c[1]); prodFornId.add(c[2]);
                    prodCustoFornecedor.add(Double.parseDouble(c[3]));
                    prodPreco.add(Double.parseDouble(c[4])); prodEstoque.add(Integer.parseInt(c[5]));
                    prodAliqAtual.add(Double.parseDouble(c[6])); prodAliqProximo.add(Double.parseDouble(c[7]));
                    prodAtivo.add(Boolean.parseBoolean(c[8]));
                } else if ("FINANCEIRO".equals(secao) && c.length >= 7) {
                    histFinData.add(c[0]); histFinTipo.add(c[1]); histFinDescricao.add(c[2]);
                    histFinValor.add(Double.parseDouble(c[3])); histFinImposto.add(Double.parseDouble(c[4]));
                    histFinLucro.add(Double.parseDouble(c[5])); histFinSaldoApos.add(Double.parseDouble(c[6]));
                } else if ("VENDAS".equals(secao) && c.length >= 10) {
                    vendaNfNum.add(Integer.parseInt(c[0])); vendaData.add(c[1]); vendaCliCpf.add(c[2]);
                    vendaCliNome.add(c[3]); vendaResumo.add(c[4]);
                    vendaSubtotal.add(Double.parseDouble(c[5])); vendaImposto.add(Double.parseDouble(c[6]));
                    vendaCustoTotal.add(Double.parseDouble(c[7])); vendaLucroLiquido.add(Double.parseDouble(c[8]));
                    vendaTotal.add(Double.parseDouble(c[9]));
                }
            }
            return !funcId.isEmpty();
        } catch (IOException | NumberFormatException | IndexOutOfBoundsException e) {
            reportarErro("ARQUIVO", "Falha ao ler backup: " + e.getMessage());
            return false;
        }
    }

    private static void inicializarSistemaComVerificacaoBackup() {
        criarArquivoTesteSeNaoExistir();
        File arqBackup = new File(ARQ_BACKUP);
        if (!arqBackup.exists()) {
            carregarBasePadrao();
            salvarTodosArquivosTxt();
            System.out.println("[INFO] Nenhum backup encontrado. Sistema iniciado com base padrao e deposito inicial.");
            return;
        }

        boolean leu = lerArquivoBackup(arqBackup);
        boolean consistente = leu && verificarConsistenciaSistema();
        if (consistente) {
            salvarTodosArquivosTxt();
            System.out.println("[OK] Backup verificado e consistente. Sistema carregado a partir do backup.");
        } else {
            System.out.println("\n[ALERTA - BACKUP INCONSISTENTE] Os dados do backup e historicos apresentam inconsistencias.");
            colocarArquivosEmQuarentena();
            carregarBasePadrao();
            salvarTodosArquivosTxt();
            System.out.println("[AVISO] Sistema aberto na base padrao SEM dados consistentes carregados da base anterior.");
            System.out.println("[DICA] Arquivo de teste disponivel em: " + ARQ_TESTE);
        }
    }

    private static void salvarTodosArquivosTxt() {
        salvarFuncionariosTxt();
        salvarClientesTxt();
        salvarProdutosTxt();
        salvarFornecedoresTxt();
        salvarHistoricoEstoqueTxt();
        salvarHistoricoFinanceiroTxt();
        salvarHistoricoAcessosTxt();
        salvarHistoricoVendasTxt();
    }

    private static void salvarFuncionariosTxt() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(ARQ_FUNCIONARIOS))) {
            for (int i = 0; i < funcId.size(); i++) {
                bw.write(String.join(";",
                        obterStringSeguro(funcId, i, ""),
                        obterStringSeguro(funcNome, i, ""),
                        obterStringSeguro(funcCpf, i, ""),
                        obterStringSeguro(funcDataNasc, i, ""),
                        obterStringSeguro(funcSexo, i, "O"),
                        obterStringSeguro(funcCargo, i, ""),
                        obterStringSeguro(funcSenha, i, ""),
                        String.valueOf(obterDoubleSeguro(funcSalario, i, 0.0)),
                        String.valueOf(obterBooleanSeguro(funcPago, i, false)),
                        String.valueOf(obterBooleanSeguro(funcAtivo, i, false)),
                        String.valueOf(obterBooleanSeguro(funcBloqueado, i, false)),
                        String.valueOf(obterIntSeguro(funcTentativas, i, 0)),
                        obterStringSeguro(funcMotivoDemissao, i, "N/A"),
                        obterStringSeguro(funcAutoridades, i, "")));
                bw.newLine();
            }
        } catch (IOException e) {
            reportarErro("ARQUIVO", "Falha ao salvar " + ARQ_FUNCIONARIOS + ": " + e.getMessage());
        }
    }

    private static void salvarClientesTxt() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(ARQ_CLIENTES))) {
            for (int i = 0; i < cliId.size(); i++) {
                bw.write(String.join(";",
                        obterStringSeguro(cliId, i, ""),
                        obterStringSeguro(cliCpf, i, ""),
                        obterStringSeguro(cliNome, i, "")));
                bw.newLine();
            }
        } catch (IOException e) {
            reportarErro("ARQUIVO", "Falha ao salvar " + ARQ_CLIENTES + ": " + e.getMessage());
        }
    }

    private static void salvarProdutosTxt() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(ARQ_PRODUTOS))) {
            for (int i = 0; i < prodId.size(); i++) {
                bw.write(String.join(";",
                        obterStringSeguro(prodId, i, ""),
                        obterStringSeguro(prodNome, i, ""),
                        obterStringSeguro(prodFornId, i, ""),
                        String.valueOf(obterDoubleSeguro(prodCustoFornecedor, i, 0.0)),
                        String.valueOf(obterDoubleSeguro(prodPreco, i, 0.0)),
                        String.valueOf(obterIntSeguro(prodEstoque, i, 0)),
                        String.valueOf(obterDoubleSeguro(prodAliqAtual, i, 0.0)),
                        String.valueOf(obterDoubleSeguro(prodAliqProximo, i, 0.0)),
                        String.valueOf(obterBooleanSeguro(prodAtivo, i, false))));
                bw.newLine();
            }
        } catch (IOException e) {
            reportarErro("ARQUIVO", "Falha ao salvar " + ARQ_PRODUTOS + ": " + e.getMessage());
        }
    }

    private static void salvarFornecedoresTxt() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(ARQ_FORNECEDORES))) {
            for (int i = 0; i < fornId.size(); i++) {
                bw.write(String.join(";",
                        obterStringSeguro(fornId, i, ""),
                        obterStringSeguro(fornNome, i, ""),
                        obterStringSeguro(fornCnpj, i, ""),
                        obterStringSeguro(fornEstado, i, ""),
                        obterStringSeguro(fornProdId, i, ""),
                        String.valueOf(obterDoubleSeguro(fornValorDevido, i, 0.0))));
                bw.newLine();
            }
        } catch (IOException e) {
            reportarErro("ARQUIVO", "Falha ao salvar " + ARQ_FORNECEDORES + ": " + e.getMessage());
        }
    }

    private static void salvarHistoricoEstoqueTxt() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(ARQ_HIST_ESTOQUE))) {
            for (int i = 0; i < histEstData.size(); i++) {
                bw.write(String.join(";",
                        obterStringSeguro(histEstData, i, ""),
                        obterStringSeguro(histEstTipo, i, ""),
                        obterStringSeguro(histEstProdId, i, ""),
                        obterStringSeguro(histEstProdNome, i, ""),
                        String.valueOf(obterIntSeguro(histEstQtd, i, 0)),
                        obterStringSeguro(histEstDetalhe, i, "")));
                bw.newLine();
            }
        } catch (IOException e) {
            reportarErro("ARQUIVO", "Falha ao salvar " + ARQ_HIST_ESTOQUE + ": " + e.getMessage());
        }
    }

    private static void salvarHistoricoFinanceiroTxt() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(ARQ_HIST_FINANCEIRO))) {
            for (int i = 0; i < histFinData.size(); i++) {
                bw.write(String.join(";",
                        obterStringSeguro(histFinData, i, ""),
                        obterStringSeguro(histFinTipo, i, ""),
                        obterStringSeguro(histFinDescricao, i, ""),
                        String.valueOf(obterDoubleSeguro(histFinValor, i, 0.0)),
                        String.valueOf(obterDoubleSeguro(histFinImposto, i, 0.0)),
                        String.valueOf(obterDoubleSeguro(histFinLucro, i, 0.0)),
                        String.valueOf(obterDoubleSeguro(histFinSaldoApos, i, 0.0))));
                bw.newLine();
            }
        } catch (IOException e) {
            reportarErro("ARQUIVO", "Falha ao salvar " + ARQ_HIST_FINANCEIRO + ": " + e.getMessage());
        }
    }

    private static void salvarHistoricoAcessosTxt() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(ARQ_HIST_ACESSOS))) {
            for (int i = 0; i < histAcData.size(); i++) {
                bw.write(String.join(";",
                        obterStringSeguro(histAcData, i, ""),
                        obterStringSeguro(histAcUsuario, i, ""),
                        obterStringSeguro(histAcAcao, i, "")));
                bw.newLine();
            }
        } catch (IOException e) {
            reportarErro("ARQUIVO", "Falha ao salvar " + ARQ_HIST_ACESSOS + ": " + e.getMessage());
        }
    }

    private static void salvarHistoricoVendasTxt() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(ARQ_HIST_VENDAS))) {
            for (int i = 0; i < vendaNfNum.size(); i++) {
                bw.write(String.join(";",
                        String.valueOf(obterIntSeguro(vendaNfNum, i, 0)),
                        obterStringSeguro(vendaData, i, ""),
                        obterStringSeguro(vendaCliCpf, i, ""),
                        obterStringSeguro(vendaCliNome, i, ""),
                        obterStringSeguro(vendaResumo, i, ""),
                        String.valueOf(obterDoubleSeguro(vendaSubtotal, i, 0.0)),
                        String.valueOf(obterDoubleSeguro(vendaImposto, i, 0.0)),
                        String.valueOf(obterDoubleSeguro(vendaCustoTotal, i, 0.0)),
                        String.valueOf(obterDoubleSeguro(vendaLucroLiquido, i, 0.0)),
                        String.valueOf(obterDoubleSeguro(vendaTotal, i, 0.0))));
                bw.newLine();
            }
        } catch (IOException e) {
            reportarErro("ARQUIVO", "Falha ao salvar " + ARQ_HIST_VENDAS + ": " + e.getMessage());
        }
    }

    private static boolean salvarBackupCompleto(String motivo) {
        salvarTodosArquivosTxt();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(ARQ_BACKUP))) {
            bw.write("# BACKUP COMPLETO - " + sanitizarTextoTxt(motivo) + " - "
                    + LocalDateTime.now().format(FMT_DATA_HORA) + "\n");
            bw.write("[CONTROLE]\nSALDO=" + saldoCaixa + "\nPROX_NF=" + proximoNf + "\n");
            bw.write("[FUNCIONARIOS]\n");
            for (int i = 0; i < funcId.size(); i++) {
                bw.write(String.join(";",
                        obterStringSeguro(funcId, i, ""),
                        obterStringSeguro(funcNome, i, ""),
                        obterStringSeguro(funcCpf, i, ""),
                        obterStringSeguro(funcDataNasc, i, ""),
                        obterStringSeguro(funcSexo, i, "O"),
                        obterStringSeguro(funcCargo, i, ""),
                        obterStringSeguro(funcSenha, i, ""),
                        String.valueOf(obterDoubleSeguro(funcSalario, i, 0.0)),
                        String.valueOf(obterBooleanSeguro(funcPago, i, false)),
                        String.valueOf(obterBooleanSeguro(funcAtivo, i, false)),
                        String.valueOf(obterBooleanSeguro(funcBloqueado, i, false)),
                        String.valueOf(obterIntSeguro(funcTentativas, i, 0)),
                        obterStringSeguro(funcMotivoDemissao, i, "N/A"),
                        obterStringSeguro(funcAutoridades, i, "")) + "\n");
            }
            bw.write("[CLIENTES]\n");
            for (int i = 0; i < cliId.size(); i++) {
                bw.write(String.join(";",
                        obterStringSeguro(cliId, i, ""),
                        obterStringSeguro(cliCpf, i, ""),
                        obterStringSeguro(cliNome, i, "")) + "\n");
            }
            bw.write("[FORNECEDORES]\n");
            for (int i = 0; i < fornId.size(); i++) {
                bw.write(String.join(";",
                        obterStringSeguro(fornId, i, ""),
                        obterStringSeguro(fornNome, i, ""),
                        obterStringSeguro(fornCnpj, i, ""),
                        obterStringSeguro(fornEstado, i, ""),
                        obterStringSeguro(fornProdId, i, ""),
                        String.valueOf(obterDoubleSeguro(fornValorDevido, i, 0.0))) + "\n");
            }
            bw.write("[PRODUTOS]\n");
            for (int i = 0; i < prodId.size(); i++) {
                bw.write(String.join(";",
                        obterStringSeguro(prodId, i, ""),
                        obterStringSeguro(prodNome, i, ""),
                        obterStringSeguro(prodFornId, i, ""),
                        String.valueOf(obterDoubleSeguro(prodCustoFornecedor, i, 0.0)),
                        String.valueOf(obterDoubleSeguro(prodPreco, i, 0.0)),
                        String.valueOf(obterIntSeguro(prodEstoque, i, 0)),
                        String.valueOf(obterDoubleSeguro(prodAliqAtual, i, 0.0)),
                        String.valueOf(obterDoubleSeguro(prodAliqProximo, i, 0.0)),
                        String.valueOf(obterBooleanSeguro(prodAtivo, i, false))) + "\n");
            }
            bw.write("[FINANCEIRO]\n");
            for (int i = 0; i < histFinData.size(); i++) {
                bw.write(String.join(";",
                        obterStringSeguro(histFinData, i, ""),
                        obterStringSeguro(histFinTipo, i, ""),
                        obterStringSeguro(histFinDescricao, i, ""),
                        String.valueOf(obterDoubleSeguro(histFinValor, i, 0.0)),
                        String.valueOf(obterDoubleSeguro(histFinImposto, i, 0.0)),
                        String.valueOf(obterDoubleSeguro(histFinLucro, i, 0.0)),
                        String.valueOf(obterDoubleSeguro(histFinSaldoApos, i, 0.0))) + "\n");
            }
            bw.write("[VENDAS]\n");
            for (int i = 0; i < vendaNfNum.size(); i++) {
                bw.write(String.join(";",
                        String.valueOf(obterIntSeguro(vendaNfNum, i, 0)),
                        obterStringSeguro(vendaData, i, ""),
                        obterStringSeguro(vendaCliCpf, i, ""),
                        obterStringSeguro(vendaCliNome, i, ""),
                        obterStringSeguro(vendaResumo, i, ""),
                        String.valueOf(obterDoubleSeguro(vendaSubtotal, i, 0.0)),
                        String.valueOf(obterDoubleSeguro(vendaImposto, i, 0.0)),
                        String.valueOf(obterDoubleSeguro(vendaCustoTotal, i, 0.0)),
                        String.valueOf(obterDoubleSeguro(vendaLucroLiquido, i, 0.0)),
                        String.valueOf(obterDoubleSeguro(vendaTotal, i, 0.0))) + "\n");
            }
            return true;
        } catch (IOException e) {
            reportarErro("ARQUIVO", "Falha ao gravar backup: " + e.getMessage());
            return false;
        }
    }

    private static void executarBackupAutomaticoEncerramento() {
        if (!backupExecutadoAoSair) {
            backupExecutadoAoSair = true;
            salvarBackupCompleto("ENCERRAMENTO_AUTOMATICO");
        }
    }

    // ==================== MODULO DO CLIENTE ====================

    private static void fluxoCliente(Scanner sc) {
        String idOpcional = lerTextoObrigatorio(sc, "Identificacao opcional (ou digite 0/VOLTAR para seguir sem identificar)");
        if (idOpcional != null) {
            System.out.println("[OK] Navegando com identificacao: " + idOpcional);
        } else {
            System.out.println("[OK] Navegando sem identificacao previa (obrigatoria na finalizacao).");
        }

        List<String> carrProdId = new ArrayList<>();
        List<Integer> carrQtd = new ArrayList<>();
        String filtro = "";
        int paginaAtual = 1;

        while (true) {
            List<Integer> indicesAtivos = new ArrayList<>();
            for (int i = 0; i < prodId.size(); i++) {
                if (!obterBooleanSeguro(prodAtivo, i, false)) {
                    continue;
                }
                String pCod = obterStringSeguro(prodId, i, "");
                String pNm = obterStringSeguro(prodNome, i, "");
                String busca = (pCod + " " + pNm).toUpperCase();
                if (filtro.isEmpty() || busca.contains(filtro.toUpperCase())) {
                    indicesAtivos.add(i);
                }
            }

            int totalPaginas = Math.max(1, Math.min(25, (int) Math.ceil(dividirComSeguranca(indicesAtivos.size(), ITENS_POR_PAGINA))));
            if (paginaAtual > totalPaginas) {
                paginaAtual = totalPaginas;
            }

            System.out.println("\n--- CATALOGO DO CLIENTE | Filtro: [" + (filtro.isEmpty() ? "NENHUM" : filtro)
                    + "] | Pagina " + paginaAtual + "/" + totalPaginas + " ---");
            int inicio = (paginaAtual - 1) * ITENS_POR_PAGINA;
            int fim = Math.min(inicio + ITENS_POR_PAGINA, indicesAtivos.size());
            if (indicesAtivos.isEmpty()) {
                System.out.println("Nenhum produto encontrado.");
            } else {
                for (int k = inicio; k < fim; k++) {
                    int idxP = obterIntSeguro(indicesAtivos, k, -1);
                    if (idxP < 0) continue;
                    System.out.printf("ID: %-8s | %-28s | Preco: %-11s | Imp: %4.1f%% | Estoque: %d%n",
                            obterStringSeguro(prodId, idxP, ""),
                            obterStringSeguro(prodNome, idxP, ""),
                            FMT_MOEDA.format(obterDoubleSeguro(prodPreco, idxP, 0.0)),
                            obterDoubleSeguro(prodAliqAtual, idxP, 0.0),
                            obterIntSeguro(prodEstoque, idxP, 0));
                }
            }

            System.out.println("1-Adicionar ao carrinho | 2-Ver/Alterar/Retirar carrinho | 3-Criar filtro | 4-Limpar filtro");
            System.out.println("5-Pagina anterior | 6-Proxima pagina | 7-Finalizar compra | 8-Voltar ao inicio");
            int op = lerInteiroFaixa(sc, "Escolha", 1, 8, false);
            if (op == Integer.MIN_VALUE) {
                continue;
            }

            if (op == 1) {
                String cod = lerTextoObrigatorio(sc, "ID do produto");
                if (cod == null) continue;
                int idxP = prodId.indexOf(cod.toUpperCase());
                if (idxP == -1 || !obterBooleanSeguro(prodAtivo, idxP, false)) {
                    reportarErro("PRODUTO INEXISTENTE", "Produto nao existe ou nao esta disponivel.");
                    continue;
                }
                int qtd = lerInteiroFaixa(sc, "Quantidade", 1, 999999, true);
                if (qtd == Integer.MIN_VALUE) continue;
                int jaNoCarr = 0;
                String codReal = obterStringSeguro(prodId, idxP, "");
                int posCarr = carrProdId.indexOf(codReal);
                if (posCarr != -1) {
                    jaNoCarr = obterIntSeguro(carrQtd, posCarr, 0);
                }
                int estDisp = obterIntSeguro(prodEstoque, idxP, 0);
                if (jaNoCarr + qtd > estDisp) {
                    reportarErro("ESTOQUE INSUFICIENTE", "Quantidade ultrapassa o estoque disponivel (" + estDisp + ").");
                    continue;
                }
                if (posCarr != -1) {
                    definirIntSeguro(carrQtd, posCarr, jaNoCarr + qtd);
                } else {
                    carrProdId.add(codReal);
                    carrQtd.add(qtd);
                }
                System.out.println("[OK] Produto adicionado ao carrinho.");
            } else if (op == 2) {
                gerenciarCarrinhoCliente(sc, carrProdId, carrQtd);
            } else if (op == 3) {
                String novoFiltro = lerTextoObrigatorio(sc, "Palavra-chave para filtrar");
                if (novoFiltro != null) {
                    filtro = novoFiltro;
                    paginaAtual = 1;
                }
            } else if (op == 4) {
                filtro = "";
                paginaAtual = 1;
                System.out.println("[OK] Filtro limpo.");
            } else if (op == 5) {
                if (paginaAtual > 1) paginaAtual--;
                else System.out.println("[AVISO] Ja esta na primeira pagina.");
            } else if (op == 6) {
                if (paginaAtual < totalPaginas) paginaAtual++;
                else System.out.println("[AVISO] Ja esta na ultima pagina.");
            } else if (op == 7) {
                if (finalizarCompraCliente(sc, carrProdId, carrQtd)) {
                    carrProdId.clear();
                    carrQtd.clear();
                }
            } else if (op == 8) {
                return;
            }
        }
    }

    private static void gerenciarCarrinhoCliente(Scanner sc, List<String> carrProdId, List<Integer> carrQtd) {
        while (true) {
            double totalGeral = 0.0;
            System.out.println("\n--- CARRINHO ---");
            if (carrProdId.isEmpty()) {
                System.out.println("Carrinho vazio. Total: " + FMT_MOEDA.format(0.0));
                return;
            }
            for (int i = 0; i < carrProdId.size(); i++) {
                String cId = obterStringSeguro(carrProdId, i, "");
                int qItem = obterIntSeguro(carrQtd, i, 0);
                int idxP = prodId.indexOf(cId);
                if (idxP == -1) continue;
                double precoUn = obterDoubleSeguro(prodPreco, idxP, 0.0);
                double aliq = obterDoubleSeguro(prodAliqAtual, idxP, 0.0);
                double sub = precoUn * qItem;
                double imp = sub * (aliq / 100.0);
                double totItem = sub + imp;
                totalGeral += totItem;
                System.out.printf("%d) %-8s | %-25s | Qtd: %3d | Unit: %-10s | Total c/ imp: %s%n",
                        (i + 1), obterStringSeguro(prodId, idxP, ""), obterStringSeguro(prodNome, idxP, ""),
                        qItem, FMT_MOEDA.format(precoUn), FMT_MOEDA.format(totItem));
            }
            System.out.println("Preco total do carrinho (com imposto): " + FMT_MOEDA.format(totalGeral));
            System.out.println("1-Alterar quantidade | 2-Retirar produto | 3-Voltar ao catalogo");
            int op = lerInteiroFaixa(sc, "Escolha", 1, 3, false);
            if (op == Integer.MIN_VALUE || op == 3) {
                return;
            }
            int itemNum = lerInteiroFaixa(sc, "Numero do item no carrinho", 1, carrProdId.size(), true);
            if (itemNum == Integer.MIN_VALUE) continue;
            int pos = itemNum - 1;
            if (pos < 0 || pos >= carrProdId.size() || pos >= carrQtd.size()) {
                reportarErro("ACESSO INDEVIDO A VETOR/INDICE", "Item do carrinho invalido.");
                continue;
            }
            int idxP = prodId.indexOf(obterStringSeguro(carrProdId, pos, ""));
            if (op == 1) {
                int novaQtd = lerInteiroFaixa(sc, "Nova quantidade (0 para retirar)", 0, 999999, false);
                if (novaQtd == Integer.MIN_VALUE) continue;
                int estDisp = idxP != -1 ? obterIntSeguro(prodEstoque, idxP, 0) : 0;
                if (novaQtd == 0) {
                    carrProdId.remove(pos);
                    carrQtd.remove(pos);
                    System.out.println("[OK] Item retirado do carrinho.");
                } else if (idxP != -1 && novaQtd > estDisp) {
                    reportarErro("ESTOQUE INSUFICIENTE", "Quantidade ultrapassa o estoque (" + estDisp + ").");
                } else {
                    definirIntSeguro(carrQtd, pos, novaQtd);
                    System.out.println("[OK] Quantidade alterada.");
                }
            } else if (op == 2) {
                carrProdId.remove(pos);
                carrQtd.remove(pos);
                System.out.println("[OK] Produto retirado do carrinho.");
            }
        }
    }

    private static boolean finalizarCompraCliente(Scanner sc, List<String> carrProdId, List<Integer> carrQtd) {
        if (carrProdId.isEmpty()) {
            reportarErro("CARRINHO VAZIO", "Adicione produtos antes de finalizar.");
            return false;
        }
        System.out.println("\n--- FINALIZACAO DE COMPRA (CPF E NOME OBRIGATORIOS) ---");
        String cpf = lerCpfValido(sc);
        if (cpf == null) {
            System.out.println("[CANCELADO] Finalizacao cancelada pelo cliente.");
            return false;
        }

        String nome;
        int idxCli = cliCpf.indexOf(cpf);
        if (idxCli != -1) {
            nome = obterStringSeguro(cliNome, idxCli, "");
            System.out.println("[INFO] Cliente ja cadastrado: " + nome + " (" + obterStringSeguro(cliId, idxCli, "") + ")");
        } else {
            nome = lerTextoObrigatorio(sc, "Nome completo do novo cliente");
            if (nome == null) {
                System.out.println("[CANCELADO] Finalizacao cancelada sem cadastro obrigatorio.");
                return false;
            }
            String novoId = String.format("CLI-%03d", cliId.size() + 1);
            cliId.add(novoId);
            cliCpf.add(cpf);
            cliNome.add(nome);
            salvarClientesTxt();
            System.out.println("[OK] Cliente registrado com ID: " + novoId);
        }

        for (int i = 0; i < carrProdId.size(); i++) {
            String cId = obterStringSeguro(carrProdId, i, "");
            int qtdItem = obterIntSeguro(carrQtd, i, 0);
            int idxP = prodId.indexOf(cId);
            if (idxP == -1 || !obterBooleanSeguro(prodAtivo, idxP, false)) {
                reportarErro("PRODUTO INEXISTENTE", "Produto inexistente no carrinho: " + cId);
                return false;
            }
            double custoUn = obterDoubleSeguro(prodCustoFornecedor, idxP, 0.0);
            double precoUn = obterDoubleSeguro(prodPreco, idxP, 0.0);
            int estDisp = obterIntSeguro(prodEstoque, idxP, 0);
            if (precoUn <= custoUn) {
                reportarErro("PRECO INVALIDO", "Produto " + cId + " possui preco menor ou igual ao custo do fornecedor.");
                return false;
            }
            if (qtdItem > estDisp) {
                reportarErro("ESTOQUE INSUFICIENTE", "Estoque insuficiente para " + obterStringSeguro(prodNome, idxP, "")
                        + " (disponivel: " + estDisp + ").");
                return false;
            }
        }

        double subtotal = 0.0;
        double impostoTotal = 0.0;
        double custoTotalVenda = 0.0;
        StringBuilder resumo = new StringBuilder();
        StringBuilder linhasNf = new StringBuilder();

        for (int i = 0; i < carrProdId.size(); i++) {
            int idxP = prodId.indexOf(obterStringSeguro(carrProdId, i, ""));
            int qtd = obterIntSeguro(carrQtd, i, 0);
            double precoUn = obterDoubleSeguro(prodPreco, idxP, 0.0);
            double aliq = obterDoubleSeguro(prodAliqAtual, idxP, 0.0);
            double custoUn = obterDoubleSeguro(prodCustoFornecedor, idxP, 0.0);
            int estAtual = obterIntSeguro(prodEstoque, idxP, 0);

            double subItem = precoUn * qtd;
            double impItem = subItem * (aliq / 100.0);
            double custoItem = custoUn * qtd;
            subtotal += subItem;
            impostoTotal += impItem;
            custoTotalVenda += custoItem;

            definirIntSeguro(prodEstoque, idxP, estAtual - qtd);
            registrarHistEstoque("VENDA_CLIENTE", obterStringSeguro(prodId, idxP, ""),
                    obterStringSeguro(prodNome, idxP, ""), qtd, "NF #" + proximoNf + " - CPF " + cpf);

            if (resumo.length() > 0) resumo.append(", ");
            resumo.append(qtd).append("x ").append(obterStringSeguro(prodId, idxP, ""));

            linhasNf.append(String.format("  %-26s | Qtd: %3d | Sub: %-10s | Imp (%4.1f%%): %s%n",
                    obterStringSeguro(prodNome, idxP, ""), qtd, FMT_MOEDA.format(subItem),
                    aliq, FMT_MOEDA.format(impItem)));
        }

        double total = subtotal + impostoTotal;
        double lucroLiquidoVenda = subtotal - custoTotalVenda;
        int nf = proximoNf++;
        String dataHora = LocalDateTime.now().format(FMT_DATA_HORA);

        saldoCaixa += total;
        registrarHistFinanceiro("VENDA",
                "NF #" + nf + " - Cliente " + nome + " (Custo: " + FMT_MOEDA.format(custoTotalVenda) + ")",
                total, impostoTotal, lucroLiquidoVenda, saldoCaixa);

        vendaNfNum.add(nf);
        vendaData.add(dataHora);
        vendaCliCpf.add(cpf);
        vendaCliNome.add(nome);
        vendaResumo.add(resumo.toString());
        vendaSubtotal.add(subtotal);
        vendaImposto.add(impostoTotal);
        vendaCustoTotal.add(custoTotalVenda);
        vendaLucroLiquido.add(lucroLiquidoVenda);
        vendaTotal.add(total);

        salvarProdutosTxt();
        salvarHistoricoVendasTxt();
        salvarBackupCompleto("VENDA_NF_" + nf);

        System.out.println("\n================ NOTA FISCAL #" + nf + " ================");
        System.out.println("Data/Hora : " + dataHora);
        System.out.println("Cliente   : " + nome + " | CPF: " + cpf);
        System.out.print(linhasNf);
        System.out.println("Subtotal  : " + FMT_MOEDA.format(subtotal));
        System.out.println("Impostos  : " + FMT_MOEDA.format(impostoTotal));
        System.out.println("TOTAL NF  : " + FMT_MOEDA.format(total));
        System.out.println("====================================================");
        return true;
    }

    // ==================== MODULO DO FUNCIONARIO ====================

    private static boolean loginFuncionario(Scanner sc) {
        String id = lerIdValido(sc, "ID do funcionario");
        if (id == null) {
            return false;
        }
        int idx = funcId.indexOf(id);
        if (idx == -1) {
            reportarErro("LOGIN", "Usuario nao cadastrado.");
            return false;
        }
        if (!obterBooleanSeguro(funcAtivo, idx, false)) {
            reportarErro("LOGIN", "Funcionario desativado. Acesso negado.");
            return false;
        }
        if (obterBooleanSeguro(funcBloqueado, idx, false)) {
            reportarErro("CONTA BLOQUEADA", "Login bloqueado apos tentativas invalidas. Solicite desbloqueio a TI.");
            return false;
        }

        while (obterIntSeguro(funcTentativas, idx, 0) < MAX_TENTATIVAS_LOGIN) {
            int tentativaAtual = obterIntSeguro(funcTentativas, idx, 0) + 1;
            String senha = lerSenhaValida(sc, "Senha (tentativa " + tentativaAtual + "/" + MAX_TENTATIVAS_LOGIN + ")", false);
            if (senha == null) {
                return false;
            }
            if (senha.equals(obterStringSeguro(funcSenha, idx, ""))) {
                definirIntSeguro(funcTentativas, idx, 0);
                usuarioLogadoId = id;
                salvarFuncionariosTxt();
                registrarAcesso(id, "Login realizado com sucesso (Cargo: " + obterStringSeguro(funcCargo, idx, "") + ")");
                return true;
            }
            definirIntSeguro(funcTentativas, idx, tentativaAtual);
            if (tentativaAtual >= MAX_TENTATIVAS_LOGIN) {
                definirBooleanSeguro(funcBloqueado, idx, true);
                salvarFuncionariosTxt();
                registrarAcesso(id, "Conta bloqueada apos 3 tentativas de senha errada");
                reportarErro("CONTA BLOQUEADA", "3 tentativas erradas. Conta bloqueada.");
                return false;
            }
            reportarErro("SENHA INVALIDA", "Senha incorreta. Tentativas restantes: " + (MAX_TENTATIVAS_LOGIN - tentativaAtual));
        }
        return false;
    }

    private static void menuFuncionario(Scanner sc) {
        while (usuarioLogadoId != null) {
            int idx = funcId.indexOf(usuarioLogadoId);
            if (!funcionarioEstaValido(idx)) {
                reportarErro("SESSAO ENCERRADA", "Conta inativa ou bloqueada.");
                usuarioLogadoId = null;
                return;
            }

            System.out.println("\n=== MENU DO FUNCIONARIO | " + obterStringSeguro(funcNome, idx, "")
                    + " (" + obterStringSeguro(funcId, idx, "") + " - " + obterStringSeguro(funcCargo, idx, "") + ") ===");
            System.out.println("1 - Meu Perfil");

            List<String> opcoesAtivas = new ArrayList<>();
            List<String> codigosAcao = new ArrayList<>();

            if (possuiAutoridade(usuarioLogadoId, "ESTOQUE")) {
                opcoesAtivas.add("Estoque e Fornecedores");
                codigosAcao.add("ESTOQUE");
            }
            if (possuiAutoridade(usuarioLogadoId, "GERENCIA")) {
                opcoesAtivas.add("Gerencia (Visao Completa)");
                codigosAcao.add("GERENCIA");
            }
            if (possuiAutoridade(usuarioLogadoId, "RH")) {
                opcoesAtivas.add("RH (Funcionarios)");
                codigosAcao.add("RH");
            }
            if (possuiAutoridade(usuarioLogadoId, "CAIXA")) {
                opcoesAtivas.add("Caixa (Venda e Nota Fiscal)");
                codigosAcao.add("CAIXA");
            }
            if (possuiAutoridade(usuarioLogadoId, "FINANCEIRO")) {
                opcoesAtivas.add("Financeiro (Caixa, Salarios, Lucros e Aliquotas)");
                codigosAcao.add("FINANCEIRO");
            }
            if (possuiAutoridade(usuarioLogadoId, "TI")) {
                opcoesAtivas.add("TI (Desbloqueio, Autoridades, Logs e Backup)");
                codigosAcao.add("TI");
            }

            int proxNum = 2;
            for (String desc : opcoesAtivas) {
                System.out.println(proxNum + " - " + desc);
                proxNum++;
            }
            int opVoltarAba = proxNum;
            int opVoltarInicio = proxNum + 1;
            System.out.println(opVoltarAba + " - Voltar (deslogar sessao)");
            System.out.println(opVoltarInicio + " - Voltar ao inicio (deslogar e ir para tela inicial)");

            int escolha = lerInteiroFaixa(sc, "Escolha", 1, opVoltarInicio, false);
            if (escolha == Integer.MIN_VALUE) {
                continue;
            }
            if (escolha == 1) {
                exibirMeuPerfil(idx);
                registrarAcesso(usuarioLogadoId, "Acessou Meu Perfil");
            } else if (escolha == opVoltarAba) {
                registrarAcesso(usuarioLogadoId, "Saiu do menu do funcionario");
                usuarioLogadoId = null;
                return;
            } else if (escolha == opVoltarInicio) {
                registrarAcesso(usuarioLogadoId, "Retornou ao inicio e deslogou");
                usuarioLogadoId = null;
                return;
            } else {
                int posAcao = escolha - 2;
                if (posAcao < 0 || posAcao >= codigosAcao.size()) {
                    reportarErro("ACESSO INDEVIDO A VETOR/INDICE", "Opcao de modulo fora do intervalo.");
                    continue;
                }
                String acao = obterStringSeguro(codigosAcao, posAcao, "");
                registrarAcesso(usuarioLogadoId, "Acessou modulo " + acao);
                executarModuloInterno(sc, acao);
            }
        }
    }

    private static void exibirMeuPerfil(int idx) {
        String nasc = obterStringSeguro(funcDataNasc, idx, "");
        int idade = calcularIdade(nasc);
        System.out.println("\n--- MEU PERFIL ---");
        System.out.println("ID          : " + obterStringSeguro(funcId, idx, ""));
        System.out.println("Nome        : " + obterStringSeguro(funcNome, idx, ""));
        System.out.println("CPF         : " + obterStringSeguro(funcCpf, idx, ""));
        System.out.println("Nascimento  : " + nasc + " (Idade: " + (idade >= 0 ? idade : "N/A") + ")");
        System.out.println("Sexo        : " + obterStringSeguro(funcSexo, idx, ""));
        System.out.println("Cargo       : " + obterStringSeguro(funcCargo, idx, ""));
        System.out.println("Salario     : " + FMT_MOEDA.format(obterDoubleSeguro(funcSalario, idx, 0.0)));
        System.out.println("Pago ciclo  : " + (obterBooleanSeguro(funcPago, idx, false) ? "SIM" : "NAO"));
        System.out.println("Ativo       : " + (obterBooleanSeguro(funcAtivo, idx, false) ? "SIM" : "NAO"));
        System.out.println("Bloqueado   : " + (obterBooleanSeguro(funcBloqueado, idx, false) ? "SIM" : "NAO"));
        System.out.println("Autoridades : " + obterStringSeguro(funcAutoridades, idx, ""));
    }

    private static void executarModuloInterno(Scanner sc, String modulo) {
        switch (modulo) {
            case "ESTOQUE":
                moduloEstoque(sc);
                break;
            case "GERENCIA":
                moduloGerencia(sc);
                break;
            case "RH":
                moduloRH(sc);
                break;
            case "CAIXA":
                moduloCaixaVenda(sc);
                break;
            case "FINANCEIRO":
                moduloFinanceiro(sc);
                break;
            case "TI":
                moduloTI(sc);
                break;
            default:
                reportarErro("MODULO INVALIDO", "Modulo desconhecido: " + modulo);
        }
    }

    // ==================== MODULOS: ESTOQUE, GERENCIA, RH, CAIXA, FINANCEIRO, TI ====================

    private static void moduloEstoque(Scanner sc) {
        if (!exigirAutoridade("ESTOQUE", "Estoque")) return;
        while (true) {
            System.out.println("\n--- ESTOQUE E FORNECEDORES ---");
            for (int i = 0; i < prodId.size(); i++) {
                double custo = obterDoubleSeguro(prodCustoFornecedor, i, 0.0);
                double venda = obterDoubleSeguro(prodPreco, i, 0.0);
                double lucroBrutoUnit = venda - custo;
                System.out.printf("%-8s | %-24s | Forn: %-7s | Custo: %-9s | Venda: %-9s | Lucro/un: %-9s | Imp: %4.1f%% | Est: %3d | %s%n",
                        obterStringSeguro(prodId, i, ""),
                        obterStringSeguro(prodNome, i, ""),
                        obterStringSeguro(prodFornId, i, ""),
                        FMT_MOEDA.format(custo),
                        FMT_MOEDA.format(venda),
                        FMT_MOEDA.format(lucroBrutoUnit),
                        obterDoubleSeguro(prodAliqAtual, i, 0.0),
                        obterIntSeguro(prodEstoque, i, 0),
                        obterBooleanSeguro(prodAtivo, i, false) ? "ATIVO" : "EXCLUIDO");
            }
            System.out.println("1-Registrar entrada/saida | 2-Cadastrar produto (com fornecedor e lucro) | 3-Comprar de fornecedor e precificar");
            System.out.println("4-Excluir produto (mantem historico) | 5-Ver historico de estoque | 6-Editar produto (modal)");
            System.out.println("7-Editar fornecedor (modal) | 8-Voltar");
            int op = lerInteiroFaixa(sc, "Escolha", 1, 8, false);
            if (op == Integer.MIN_VALUE || op == 8) return;

            if (op == 1) {
                String cod = lerTextoObrigatorio(sc, "ID do produto");
                if (cod == null) continue;
                int idxP = prodId.indexOf(cod.toUpperCase());
                if (idxP == -1 || !obterBooleanSeguro(prodAtivo, idxP, false)) {
                    reportarErro("PRODUTO INEXISTENTE", "Produto inexistente ou excluido.");
                    continue;
                }
                int tipo = lerInteiroFaixa(sc, "1-Entrada ou 2-Saida", 1, 2, true);
                if (tipo == Integer.MIN_VALUE) continue;
                int qtd = lerInteiroFaixa(sc, "Quantidade", 1, 999999, true);
                if (qtd == Integer.MIN_VALUE) continue;
                int estAtual = obterIntSeguro(prodEstoque, idxP, 0);
                if (tipo == 1) {
                    definirIntSeguro(prodEstoque, idxP, estAtual + qtd);
                    registrarHistEstoque("ENTRADA", obterStringSeguro(prodId, idxP, ""),
                            obterStringSeguro(prodNome, idxP, ""), qtd, "Entrada manual");
                    System.out.println("[OK] Entrada registrada.");
                } else {
                    if (qtd > estAtual) {
                        reportarErro("ESTOQUE INSUFICIENTE", "Saldo insuficiente no estoque (" + estAtual + ").");
                        continue;
                    }
                    definirIntSeguro(prodEstoque, idxP, estAtual - qtd);
                    registrarHistEstoque("SAIDA", obterStringSeguro(prodId, idxP, ""),
                            obterStringSeguro(prodNome, idxP, ""), qtd, "Saida manual");
                    System.out.println("[OK] Saida registrada.");
                }
                salvarProdutosTxt();
            } else if (op == 2) {
                String novoProdId = lerIdValido(sc, "Novo ID do produto");
                if (novoProdId == null) continue;
                if (prodId.contains(novoProdId)) {
                    reportarErro("DUPLICIDADE", "ID de produto ja existe.");
                    continue;
                }
                String nomeP = lerTextoObrigatorio(sc, "Nome do produto");
                if (nomeP == null) continue;
                double custoF = lerDecimalMinimo(sc, "Custo unitario do fornecedor", 0.01);
                if (custoF < 0) continue;
                double aliqAt = lerDecimalMinimo(sc, "Aliquota deste ano (%)", 0.0);
                if (aliqAt < 0) continue;
                double aliqPr = lerDecimalMinimo(sc, "Aliquota do ano que vem (%)", 0.0);
                if (aliqPr < 0) continue;

                double precoP = lerPrecoVendaComLucro(sc, custoF, aliqAt);
                if (precoP < 0) continue;

                int estP = lerInteiroFaixa(sc, "Estoque inicial", 0, 999999, false);
                if (estP == Integer.MIN_VALUE) continue;

                String fId = lerIdValido(sc, "ID do fornecedor (ex: FOR-002)");
                if (fId == null) continue;
                if (fornId.contains(fId)) {
                    reportarErro("DUPLICIDADE", "ID de fornecedor ja existe.");
                    continue;
                }
                String fNome = lerTextoObrigatorio(sc, "Nome do fornecedor");
                if (fNome == null) continue;
                String fCnpj = lerCnpjValido(sc);
                if (fCnpj == null) continue;
                if (fornCnpj.contains(fCnpj)) {
                    reportarErro("DUPLICIDADE", "CNPJ de fornecedor ja cadastrado.");
                    continue;
                }
                String fUf = lerUfValida(sc);
                if (fUf == null) continue;

                prodId.add(novoProdId); prodNome.add(nomeP); prodFornId.add(fId);
                prodCustoFornecedor.add(custoF); prodPreco.add(precoP); prodEstoque.add(estP);
                prodAliqAtual.add(aliqAt); prodAliqProximo.add(aliqPr); prodAtivo.add(true);

                fornId.add(fId); fornNome.add(fNome); fornCnpj.add(fCnpj);
                fornEstado.add(fUf); fornProdId.add(novoProdId); fornValorDevido.add(0.0);

                registrarHistEstoque("CADASTRO", novoProdId, nomeP, estP,
                        "Vinculado a " + fId + " (" + fUf + ") | Custo: " + FMT_MOEDA.format(custoF)
                                + " | Venda: " + FMT_MOEDA.format(precoP));
                salvarProdutosTxt();
                salvarFornecedoresTxt();
                System.out.println("[OK] Produto e fornecedor cadastrados com preco de venda superior ao custo.");
            } else if (op == 3) {
                comprarProdutoDeFornecedor(sc);
            } else if (op == 4) {
                String cod = lerTextoObrigatorio(sc, "ID do produto para excluir");
                if (cod == null) continue;
                int idxP = prodId.indexOf(cod.toUpperCase());
                if (idxP == -1 || !obterBooleanSeguro(prodAtivo, idxP, false)) {
                    reportarErro("PRODUTO INEXISTENTE", "Produto nao encontrado ou ja excluido.");
                    continue;
                }
                definirBooleanSeguro(prodAtivo, idxP, false);
                registrarHistEstoque("EXCLUSAO", obterStringSeguro(prodId, idxP, ""),
                        obterStringSeguro(prodNome, idxP, ""), obterIntSeguro(prodEstoque, idxP, 0),
                        "Produto removido do catalogo ativo (mantido no historico)");
                salvarProdutosTxt();
                System.out.println("[OK] Produto excluido do catalogo ativo, mas preservado no historico de estoque.");
            } else if (op == 5) {
                System.out.println("\n--- HISTORICO DE ESTOQUE ---");
                for (int i = 0; i < histEstData.size(); i++) {
                    System.out.printf("%s | %-15s | %-8s | Qtd: %3d | %s%n",
                            obterStringSeguro(histEstData, i, ""),
                            obterStringSeguro(histEstTipo, i, ""),
                            obterStringSeguro(histEstProdId, i, ""),
                            obterIntSeguro(histEstQtd, i, 0),
                            obterStringSeguro(histEstDetalhe, i, ""));
                }
            } else if (op == 6) {
                modalEditarProduto(sc);
            } else if (op == 7) {
                modalEditarFornecedor(sc);
            }
        }
    }

    private static void modalEditarProduto(Scanner sc) {
        if (!exigirAutoridade("ESTOQUE", "Editar Produto (Modal)")) {
            return;
        }
        String cod = lerTextoObrigatorio(sc, "ID do produto para editar");
        if (cod == null) {
            return;
        }
        int idxP = prodId.indexOf(cod.toUpperCase());
        if (idxP == -1) {
            reportarErro("PRODUTO INEXISTENTE", "Produto nao encontrado.");
            return;
        }

        while (true) {
            double custoAtual = obterDoubleSeguro(prodCustoFornecedor, idxP, 0.0);
            double vendaAtual = obterDoubleSeguro(prodPreco, idxP, 0.0);
            double aliqAtAtual = obterDoubleSeguro(prodAliqAtual, idxP, 0.0);
            double aliqPrAtual = obterDoubleSeguro(prodAliqProximo, idxP, 0.0);
            double lucroLiqUn = calcularLucroLiquidoUnitario(vendaAtual, custoAtual, aliqAtAtual);

            System.out.println("\n[MODAL EDITAR PRODUTO: " + obterStringSeguro(prodId, idxP, "") + "]");
            System.out.println("1 - Nome          : " + obterStringSeguro(prodNome, idxP, ""));
            System.out.println("2 - Custo Forn.   : " + FMT_MOEDA.format(custoAtual));
            System.out.println("3 - Preco Venda   : " + FMT_MOEDA.format(vendaAtual)
                    + " (Lucro liq/un: " + FMT_MOEDA.format(lucroLiqUn) + ")");
            System.out.printf("4 - Aliquotas     : Atual %.2f%% | Proximo ano %.2f%%%n", aliqAtAtual, aliqPrAtual);
            System.out.println("5 - Salvar e sair | 0/VOLTAR - Cancelar");

            int campo = lerInteiroFaixa(sc, "Escolha o campo", 1, 5, true);
            if (campo == Integer.MIN_VALUE) {
                System.out.println("[CANCELADO] Edicao de produto cancelada.");
                return;
            }
            if (campo == 1) {
                String novoNome = lerTextoObrigatorio(sc, "Novo nome do produto");
                if (novoNome != null) {
                    definirStringSeguro(prodNome, idxP, novoNome);
                }
            } else if (campo == 2) {
                double novoCusto = lerDecimalMinimo(sc, "Novo custo unitario do fornecedor", 0.01);
                if (novoCusto >= 0) {
                    definirDoubleSeguro(prodCustoFornecedor, idxP, novoCusto);
                    if (obterDoubleSeguro(prodPreco, idxP, 0.0) <= novoCusto) {
                        System.out.println("[AVISO] O preco de venda atual ficou menor ou igual ao novo custo.");
                        double novoPreco = lerPrecoVendaComLucro(sc, novoCusto, obterDoubleSeguro(prodAliqAtual, idxP, 0.0));
                        if (novoPreco > novoCusto) {
                            definirDoubleSeguro(prodPreco, idxP, novoPreco);
                        } else {
                            System.out.println("[AVISO] Ajuste de preco cancelado; ajuste o preco antes de salvar.");
                        }
                    }
                }
            } else if (campo == 3) {
                double novoPreco = lerPrecoVendaComLucro(sc,
                        obterDoubleSeguro(prodCustoFornecedor, idxP, 0.0),
                        obterDoubleSeguro(prodAliqAtual, idxP, 0.0));
                if (novoPreco > obterDoubleSeguro(prodCustoFornecedor, idxP, 0.0)) {
                    definirDoubleSeguro(prodPreco, idxP, novoPreco);
                }
            } else if (campo == 4) {
                double novaAliqAt = lerDecimalMinimo(sc, "Nova aliquota deste ano (%)", 0.0);
                if (novaAliqAt < 0) continue;
                double novaAliqPr = lerDecimalMinimo(sc, "Nova aliquota do ano que vem (%)", 0.0);
                if (novaAliqPr < 0) continue;
                definirDoubleSeguro(prodAliqAtual, idxP, novaAliqAt);
                definirDoubleSeguro(prodAliqProximo, idxP, novaAliqPr);
            } else if (campo == 5) {
                if (obterDoubleSeguro(prodPreco, idxP, 0.0) <= obterDoubleSeguro(prodCustoFornecedor, idxP, 0.0)) {
                    reportarErro("PRECO INVALIDO", "Preco de venda nao pode ser menor ou igual ao custo do fornecedor.");
                    continue;
                }
                salvarProdutosTxt();
                salvarBackupCompleto("MODAL_EDITAR_PRODUTO_" + obterStringSeguro(prodId, idxP, ""));
                System.out.println("[OK] Produto atualizado e sincronizado nos .TXT.");
                return;
            }
        }
    }

    private static void modalEditarFornecedor(Scanner sc) {
        if (!exigirAutoridade("ESTOQUE", "Editar Fornecedor (Modal)")) {
            return;
        }
        String codF = lerIdValido(sc, "ID do fornecedor para editar");
        if (codF == null) {
            return;
        }
        int idxF = fornId.indexOf(codF);
        if (idxF == -1) {
            reportarErro("FORNECEDOR INEXISTENTE", "Fornecedor nao encontrado.");
            return;
        }

        while (true) {
            System.out.println("\n[MODAL EDITAR FORNECEDOR: " + obterStringSeguro(fornId, idxF, "") + "]");
            System.out.println("1 - Nome / Razao Social : " + obterStringSeguro(fornNome, idxF, ""));
            System.out.println("2 - CNPJ                : " + obterStringSeguro(fornCnpj, idxF, ""));
            System.out.println("3 - Estado (UF)         : " + obterStringSeguro(fornEstado, idxF, ""));
            System.out.println("4 - Produto Vinculado   : " + obterStringSeguro(fornProdId, idxF, ""));
            System.out.println("5 - Salvar e sair       | 0/VOLTAR - Cancelar");

            int campo = lerInteiroFaixa(sc, "Escolha o campo", 1, 5, true);
            if (campo == Integer.MIN_VALUE) {
                System.out.println("[CANCELADO] Edicao de fornecedor cancelada.");
                return;
            }
            if (campo == 1) {
                String novoNome = lerTextoObrigatorio(sc, "Novo nome do fornecedor");
                if (novoNome != null) {
                    definirStringSeguro(fornNome, idxF, novoNome);
                }
            } else if (campo == 2) {
                String novoCnpj = lerCnpjValido(sc);
                if (novoCnpj != null) {
                    boolean duplicado = false;
                    for (int i = 0; i < fornCnpj.size(); i++) {
                        if (i != idxF && novoCnpj.equals(obterStringSeguro(fornCnpj, i, ""))) {
                            duplicado = true;
                            break;
                        }
                    }
                    if (duplicado) {
                        reportarErro("DUPLICIDADE", "CNPJ ja cadastrado em outro fornecedor.");
                    } else {
                        definirStringSeguro(fornCnpj, idxF, novoCnpj);
                    }
                }
            } else if (campo == 3) {
                String novaUf = lerUfValida(sc);
                if (novaUf != null) {
                    definirStringSeguro(fornEstado, idxF, novaUf);
                }
            } else if (campo == 4) {
                String novoProd = lerIdValido(sc, "ID do produto fornecido");
                if (novoProd != null) {
                    int idxP = prodId.indexOf(novoProd);
                    if (idxP == -1) {
                        reportarErro("PRODUTO INEXISTENTE", "Produto nao encontrado no cadastro.");
                    } else {
                        definirStringSeguro(fornProdId, idxF, novoProd);
                        definirStringSeguro(prodFornId, idxP, obterStringSeguro(fornId, idxF, ""));
                        salvarProdutosTxt();
                    }
                }
            } else if (campo == 5) {
                String ufAtual = obterStringSeguro(fornEstado, idxF, "");
                String pAtual = obterStringSeguro(fornProdId, idxF, "");
                if (!UFS_VALIDAS.contains(ufAtual) || !prodId.contains(pAtual)) {
                    reportarErro("INTEGRIDADE", "UF ou produto vinculado invalido.");
                    continue;
                }
                salvarFornecedoresTxt();
                salvarBackupCompleto("MODAL_EDITAR_FORNECEDOR_" + obterStringSeguro(fornId, idxF, ""));
                System.out.println("[OK] Fornecedor atualizado e sincronizado nos .TXT.");
                return;
            }
        }
    }

    private static void comprarProdutoDeFornecedor(Scanner sc) {
        String codF = lerIdValido(sc, "ID do fornecedor");
        if (codF == null) return;
        int idxF = fornId.indexOf(codF);
        if (idxF == -1) {
            reportarErro("FORNECEDOR INEXISTENTE", "Fornecedor nao encontrado.");
            return;
        }
        String codP = obterStringSeguro(fornProdId, idxF, "");
        int idxP = prodId.indexOf(codP);
        if (idxP == -1) {
            reportarErro("PRODUTO INEXISTENTE", "Produto vinculado nao encontrado.");
            return;
        }
        int qtd = lerInteiroFaixa(sc, "Quantidade a comprar para " + obterStringSeguro(prodNome, idxP, ""), 1, 999999, true);
        if (qtd == Integer.MIN_VALUE) return;
        double custoUnit = lerDecimalMinimo(sc, "Custo unitario de compra ao fornecedor", 0.01);
        if (custoUnit < 0) return;

        double custoTotal = qtd * custoUnit;
        if (custoTotal > saldoCaixa) {
            reportarErro("SALDO INSUFICIENTE", "Custo: " + FMT_MOEDA.format(custoTotal)
                    + " | Saldo em caixa: " + FMT_MOEDA.format(saldoCaixa));
            return;
        }

        double precoAtual = obterDoubleSeguro(prodPreco, idxP, 0.0);
        double aliqAtual = obterDoubleSeguro(prodAliqAtual, idxP, 0.0);
        double novoPrecoVenda = precoAtual;

        System.out.println("\nPreco de venda atual do produto: " + FMT_MOEDA.format(precoAtual)
                + " | Novo custo fornecedor: " + FMT_MOEDA.format(custoUnit));

        if (precoAtual <= custoUnit) {
            System.out.println("[AVISO OBRIGATORIO] O preco de venda atual (" + FMT_MOEDA.format(precoAtual)
                    + ") e menor ou igual ao custo do fornecedor (" + FMT_MOEDA.format(custoUnit) + ").");
            System.out.println("Defina um novo preco de venda maior que o custo para continuar:");
            novoPrecoVenda = lerPrecoVendaComLucro(sc, custoUnit, aliqAtual);
            if (novoPrecoVenda < 0) {
                System.out.println("[CANCELADO] Compra do fornecedor cancelada sem precificacao valida.");
                return;
            }
        } else {
            System.out.println("1 - Manter preco de venda atual (" + FMT_MOEDA.format(precoAtual)
                    + ") | 2 - Ajustar preco de venda / margem de lucro");
            int opPreco = lerInteiroFaixa(sc, "Escolha", 1, 2, true);
            if (opPreco == Integer.MIN_VALUE) {
                return;
            }
            if (opPreco == 2) {
                novoPrecoVenda = lerPrecoVendaComLucro(sc, custoUnit, aliqAtual);
                if (novoPrecoVenda < 0) {
                    System.out.println("[CANCELADO] Compra do fornecedor cancelada.");
                    return;
                }
            }
        }

        if (novoPrecoVenda <= custoUnit) {
            reportarErro("PRECO INVALIDO", "Preco de venda nao pode ser menor ou igual ao custo do fornecedor.");
            return;
        }

        saldoCaixa -= custoTotal;
        definirDoubleSeguro(prodCustoFornecedor, idxP, custoUnit);
        definirDoubleSeguro(prodPreco, idxP, novoPrecoVenda);
        definirIntSeguro(prodEstoque, idxP, obterIntSeguro(prodEstoque, idxP, 0) + qtd);
        definirBooleanSeguro(prodAtivo, idxP, true);

        double lucroUnitBruto = novoPrecoVenda - custoUnit;
        double lucroUnitLiq = calcularLucroLiquidoUnitario(novoPrecoVenda, custoUnit, aliqAtual);

        registrarHistFinanceiro("FORNECEDOR",
                "Compra " + qtd + "x " + codP + " (" + codF + ") | Custo: " + FMT_MOEDA.format(custoUnit)
                        + " | Venda: " + FMT_MOEDA.format(novoPrecoVenda)
                        + " | Lucro est/un: " + FMT_MOEDA.format(lucroUnitLiq),
                custoTotal, 0.0, 0.0, saldoCaixa);
        registrarHistEstoque("COMPRA_FORN", codP, obterStringSeguro(prodNome, idxP, ""), qtd,
                "Comprado de " + codF + " | Custo: " + FMT_MOEDA.format(custoUnit)
                        + " | Novo Venda: " + FMT_MOEDA.format(novoPrecoVenda));

        salvarProdutosTxt();
        salvarBackupCompleto("COMPRA_FORNECEDOR");
        System.out.println("[OK] Compra concluida e sincronizada!");
        System.out.println("  -> Custo unitario : " + FMT_MOEDA.format(custoUnit));
        System.out.println("  -> Preco de venda : " + FMT_MOEDA.format(novoPrecoVenda));
        System.out.println("  -> Margem bruta/un: " + FMT_MOEDA.format(lucroUnitBruto)
                + " | Lucro liquido/un (apos imposto): " + FMT_MOEDA.format(lucroUnitLiq));
        System.out.println("  -> Saldo restante : " + FMT_MOEDA.format(saldoCaixa));
    }

    private static void moduloGerencia(Scanner sc) {
        if (!exigirAutoridade("GERENCIA", "Gerencia")) return;
        if (!confirmarSenhaGerencia(sc)) return;

        System.out.println("\n=== VISAO GERAL DA GERENCIA ===");
        System.out.println("Saldo atual do caixa : " + FMT_MOEDA.format(saldoCaixa));
        System.out.println("Funcionarios         : " + funcId.size());
        System.out.println("Clientes             : " + cliId.size());
        System.out.println("Fornecedores         : " + fornId.size());
        System.out.println("Produtos             : " + prodId.size());
        System.out.println("Vendas realizadas    : " + vendaNfNum.size());
        lerTextoObrigatorio(sc, "Pressione ENTER ou digite 0/VOLTAR para sair da visao");
    }

    private static void moduloRH(Scanner sc) {
        if (!exigirAutoridade("RH", "RH")) return;
        while (true) {
            System.out.println("\n--- RECURSOS HUMANOS (RH) ---");
            for (int i = 0; i < funcId.size(); i++) {
                int idade = calcularIdade(obterStringSeguro(funcDataNasc, i, ""));
                System.out.printf("%d) %-10s | %-20s | Idade: %2d | Cargo: %-8s | Sal: %-10s | Pago: %-3s | Ativo: %s%n",
                        (i + 1),
                        obterStringSeguro(funcId, i, ""),
                        obterStringSeguro(funcNome, i, ""),
                        idade,
                        obterStringSeguro(funcCargo, i, ""),
                        FMT_MOEDA.format(obterDoubleSeguro(funcSalario, i, 0.0)),
                        obterBooleanSeguro(funcPago, i, false) ? "SIM" : "NAO",
                        obterBooleanSeguro(funcAtivo, i, false) ? "SIM" : "NAO");
            }
            System.out.println("1-Contratar/Cadastrar | 2-Demitir (com motivo) | 3-Readmitir | 4-Editar info (modal) | 5-Voltar");
            int op = lerInteiroFaixa(sc, "Escolha", 1, 5, false);
            if (op == Integer.MIN_VALUE || op == 5) return;

            if (op == 1) {
                String novoId = lerIdValido(sc, "Novo ID do funcionario (ex: UR-RH02)");
                if (novoId == null) continue;
                if (funcId.contains(novoId)) {
                    reportarErro("DUPLICIDADE", "ID ja cadastrado.");
                    continue;
                }
                String nome = lerTextoObrigatorio(sc, "Nome completo");
                if (nome == null) continue;
                String cpf = lerCpfValido(sc);
                if (cpf == null) continue;
                if (funcCpf.contains(cpf)) {
                    reportarErro("DUPLICIDADE", "CPF ja cadastrado para outro funcionario.");
                    continue;
                }
                String nasc = lerDataNascimentoValida(sc);
                if (nasc == null) continue;
                String sexo = lerSexoValido(sc);
                if (sexo == null) continue;
                String cargo = lerTextoObrigatorio(sc, "Cargo (GERENCIA, RH, FIN, EST, CAIXA, TI)");
                if (cargo == null) continue;
                String senha = lerSenhaValida(sc, "Senha inicial (min. 4 caracteres)", true);
                if (senha == null) continue;
                double sal = lerDecimalMinimo(sc, "Salario", 0.01);
                if (sal < 0) continue;

                funcId.add(novoId); funcNome.add(nome); funcCpf.add(cpf); funcDataNasc.add(nasc);
                funcSexo.add(sexo); funcCargo.add(cargo.toUpperCase()); funcSenha.add(senha);
                funcSalario.add(sal); funcPago.add(false); funcAtivo.add(true);
                funcBloqueado.add(false); funcTentativas.add(0); funcMotivoDemissao.add("N/A");
                funcAutoridades.add(cargo.toUpperCase());

                salvarFuncionariosTxt();
                System.out.println("[OK] Funcionario contratado e cadastrado.");
            } else if (op == 2) {
                if (funcId.isEmpty()) continue;
                int num = lerInteiroFaixa(sc, "Numero do funcionario", 1, funcId.size(), true);
                if (num == Integer.MIN_VALUE) continue;
                int idx = num - 1;
                String alvoId = obterStringSeguro(funcId, idx, "");
                if (alvoId.equals(usuarioLogadoId)) {
                    reportarErro("ACAO BLOQUEADA", "Trava de seguranca: voce nao pode demitir a si mesmo.");
                    continue;
                }
                if ("GERENCIA".equalsIgnoreCase(obterStringSeguro(funcCargo, idx, "")) && !confirmarSenhaGerencia(sc)) {
                    continue;
                }
                String motivo = lerTextoObrigatorio(sc, "Motivo obrigatorio da demissao");
                if (motivo == null) {
                    reportarErro("MOTIVO OBRIGATORIO", "Demissao cancelada: motivo e obrigatorio.");
                    continue;
                }
                definirBooleanSeguro(funcAtivo, idx, false);
                definirStringSeguro(funcMotivoDemissao, idx, motivo);
                definirStringSeguro(funcAutoridades, idx, "NENHUMA");
                salvarFuncionariosTxt();
                System.out.println("[OK] Funcionario demitido e autoridades revogadas.");
            } else if (op == 3) {
                if (funcId.isEmpty()) continue;
                int num = lerInteiroFaixa(sc, "Numero do funcionario para readmitir", 1, funcId.size(), true);
                if (num == Integer.MIN_VALUE) continue;
                int idx = num - 1;
                definirBooleanSeguro(funcAtivo, idx, true);
                definirBooleanSeguro(funcBloqueado, idx, false);
                definirIntSeguro(funcTentativas, idx, 0);
                definirStringSeguro(funcMotivoDemissao, idx, "READMITIDO");
                definirStringSeguro(funcAutoridades, idx, obterStringSeguro(funcCargo, idx, ""));
                salvarFuncionariosTxt();
                System.out.println("[OK] Funcionario readmitido.");
            } else if (op == 4) {
                modalEditarFuncionario(sc);
            }
        }
    }

    private static void modalEditarFuncionario(Scanner sc) {
        if (funcId.isEmpty()) return;
        int num = lerInteiroFaixa(sc, "Numero do funcionario para editar", 1, funcId.size(), true);
        if (num == Integer.MIN_VALUE) return;
        int idx = num - 1;
        if ("GERENCIA".equalsIgnoreCase(obterStringSeguro(funcCargo, idx, "")) && !confirmarSenhaGerencia(sc)) {
            return;
        }
        System.out.println("\n[MODAL EDITAR FUNCIONARIO: " + obterStringSeguro(funcId, idx, "") + "]");
        System.out.println("1-Nome (" + obterStringSeguro(funcNome, idx, "")
                + ") | 2-Nascimento (" + obterStringSeguro(funcDataNasc, idx, "")
                + ") | 3-Sexo (" + obterStringSeguro(funcSexo, idx, "")
                + ") | 4-Cargo (" + obterStringSeguro(funcCargo, idx, "") + ") | 5-Cancelar");
        int campo = lerInteiroFaixa(sc, "Campo", 1, 5, false);
        if (campo == Integer.MIN_VALUE || campo == 5) return;
        if (campo == 1) {
            String novo = lerTextoObrigatorio(sc, "Novo nome");
            if (novo != null) definirStringSeguro(funcNome, idx, novo);
        } else if (campo == 2) {
            String novaData = lerDataNascimentoValida(sc);
            if (novaData != null) definirStringSeguro(funcDataNasc, idx, novaData);
        } else if (campo == 3) {
            String novoSexo = lerSexoValido(sc);
            if (novoSexo != null) definirStringSeguro(funcSexo, idx, novoSexo);
        } else if (campo == 4) {
            String novoCargo = lerTextoObrigatorio(sc, "Novo cargo");
            if (novoCargo != null) {
                String upCargo = novoCargo.toUpperCase();
                definirStringSeguro(funcCargo, idx, upCargo);
                if (obterBooleanSeguro(funcAtivo, idx, false)) {
                    definirStringSeguro(funcAutoridades, idx, upCargo);
                }
            }
        }
        salvarFuncionariosTxt();
        System.out.println("[OK] Alteracao salva apos validacao.");
    }

    private static void moduloCaixaVenda(Scanner sc) {
        if (!exigirAutoridade("CAIXA", "Caixa")) return;
        List<String> carrProd = new ArrayList<>();
        List<Integer> carrQtd = new ArrayList<>();
        while (true) {
            System.out.println("\n--- CAIXA ---");
            System.out.println("1-Adicionar item | 2-Finalizar venda e emitir NF | 3-Voltar");
            int op = lerInteiroFaixa(sc, "Escolha", 1, 3, false);
            if (op == Integer.MIN_VALUE || op == 3) return;
            if (op == 1) {
                String cod = lerTextoObrigatorio(sc, "ID do produto");
                if (cod == null) continue;
                int idxP = prodId.indexOf(cod.toUpperCase());
                if (idxP == -1 || !obterBooleanSeguro(prodAtivo, idxP, false)) {
                    reportarErro("PRODUTO INEXISTENTE", "Produto inexistente.");
                    continue;
                }
                int qtd = lerInteiroFaixa(sc, "Quantidade", 1, 999999, true);
                if (qtd == Integer.MIN_VALUE) continue;
                int estDisp = obterIntSeguro(prodEstoque, idxP, 0);
                if (qtd > estDisp) {
                    reportarErro("ESTOQUE INSUFICIENTE", "Estoque insuficiente (" + estDisp + ").");
                    continue;
                }
                carrProd.add(obterStringSeguro(prodId, idxP, ""));
                carrQtd.add(qtd);
                System.out.println("[OK] Item adicionado ao caixa.");
            } else if (op == 2) {
                if (finalizarCompraCliente(sc, carrProd, carrQtd)) {
                    carrProd.clear();
                    carrQtd.clear();
                }
            }
        }
    }

    private static void moduloFinanceiro(Scanner sc) {
        if (!exigirAutoridade("FINANCEIRO", "Financeiro")) return;
        while (true) {
            double totalVendasBruto = 0.0;
            double totalImpostosVendas = 0.0;
            double totalCustoVendas = 0.0;
            double totalLucroVendas = 0.0;
            for (int i = 0; i < vendaNfNum.size(); i++) {
                totalVendasBruto += obterDoubleSeguro(vendaTotal, i, 0.0);
                totalImpostosVendas += obterDoubleSeguro(vendaImposto, i, 0.0);
                totalCustoVendas += obterDoubleSeguro(vendaCustoTotal, i, 0.0);
                totalLucroVendas += obterDoubleSeguro(vendaLucroLiquido, i, 0.0);
            }

            System.out.println("\n--- FINANCEIRO | Saldo em caixa: " + FMT_MOEDA.format(saldoCaixa) + " ---");
            System.out.println("Resumo Vendas -> Total recebido: " + FMT_MOEDA.format(totalVendasBruto)
                    + " | Impostos: " + FMT_MOEDA.format(totalImpostosVendas)
                    + " | Custo mercadoria: " + FMT_MOEDA.format(totalCustoVendas)
                    + " | Lucro liquido apurado: " + FMT_MOEDA.format(totalLucroVendas));
            System.out.println("1-Pagar salario funcionario | 2-Ajustar salario | 3-Comprar/Pagar fornecedor (com lucro)");
            System.out.println("4-Definir aliquotas (ano atual / proximo ano) e revisar preco | 5-Historico financeiro detalhado | 6-Voltar");
            int op = lerInteiroFaixa(sc, "Escolha", 1, 6, false);
            if (op == Integer.MIN_VALUE || op == 6) return;

            if (op == 1) {
                String idF = lerIdValido(sc, "ID do funcionario para pagar");
                if (idF == null) continue;
                int idx = funcId.indexOf(idF);
                if (idx == -1) {
                    reportarErro("FUNCIONARIO INEXISTENTE", "Funcionario nao encontrado.");
                    continue;
                }
                if (obterBooleanSeguro(funcPago, idx, false)) {
                    reportarErro("PAGAMENTO DUPLICADO", "Este funcionario JA esta pago no ciclo atual.");
                    continue;
                }
                double sal = obterDoubleSeguro(funcSalario, idx, 0.0);
                if (sal > saldoCaixa) {
                    reportarErro("SALDO INSUFICIENTE", "Saldo insuficiente em caixa para pagar " + FMT_MOEDA.format(sal) + ".");
                    continue;
                }
                saldoCaixa -= sal;
                definirBooleanSeguro(funcPago, idx, true);
                registrarHistFinanceiro("SALARIO",
                        "Pagamento salario de " + obterStringSeguro(funcNome, idx, "") + " (" + idF + ")",
                        sal, 0.0, 0.0, saldoCaixa);
                salvarFuncionariosTxt();
                System.out.println("[OK] Salario pago. Novo saldo: " + FMT_MOEDA.format(saldoCaixa));
            } else if (op == 2) {
                String idF = lerIdValido(sc, "ID do funcionario");
                if (idF == null) continue;
                int idx = funcId.indexOf(idF);
                if (idx == -1) {
                    reportarErro("FUNCIONARIO INEXISTENTE", "Funcionario nao encontrado.");
                    continue;
                }
                if ("GERENCIA".equalsIgnoreCase(obterStringSeguro(funcCargo, idx, "")) && !confirmarSenhaGerencia(sc)) {
                    continue;
                }
                double novoSal = lerDecimalMinimo(sc, "Novo salario", 0.01);
                if (novoSal >= 0) {
                    definirDoubleSeguro(funcSalario, idx, novoSal);
                    salvarFuncionariosTxt();
                    System.out.println("[OK] Salario ajustado.");
                }
            } else if (op == 3) {
                comprarProdutoDeFornecedor(sc);
            } else if (op == 4) {
                String codP = lerTextoObrigatorio(sc, "ID do produto");
                if (codP == null) continue;
                int idxP = prodId.indexOf(codP.toUpperCase());
                if (idxP == -1) {
                    reportarErro("PRODUTO INEXISTENTE", "Produto nao encontrado.");
                    continue;
                }
                double aliqAtual = lerDecimalMinimo(sc, "Aliquota deste ano (%)", 0.0);
                if (aliqAtual < 0) continue;
                double aliqProx = lerDecimalMinimo(sc, "Aliquota do ano que vem (%)", 0.0);
                if (aliqProx < 0) continue;

                definirDoubleSeguro(prodAliqAtual, idxP, aliqAtual);
                definirDoubleSeguro(prodAliqProximo, idxP, aliqProx);

                double lucroLiqUn = calcularLucroLiquidoUnitario(
                        obterDoubleSeguro(prodPreco, idxP, 0.0),
                        obterDoubleSeguro(prodCustoFornecedor, idxP, 0.0),
                        aliqAtual);
                System.out.println("[INFO] Com a nova aliquota de " + aliqAtual + "%, o lucro liquido/un e: "
                        + FMT_MOEDA.format(lucroLiqUn));
                System.out.println("Deseja ajustar o preco de venda agora? 1-Sim | 2-Nao");
                int aj = lerInteiroFaixa(sc, "Escolha", 1, 2, false);
                if (aj == 1) {
                    double novoPreco = lerPrecoVendaComLucro(sc, obterDoubleSeguro(prodCustoFornecedor, idxP, 0.0), aliqAtual);
                    if (novoPreco > obterDoubleSeguro(prodCustoFornecedor, idxP, 0.0)) {
                        definirDoubleSeguro(prodPreco, idxP, novoPreco);
                    }
                }
                salvarProdutosTxt();
                System.out.println("[OK] Aliquotas e precificacao atualizadas.");
            } else if (op == 5) {
                System.out.println("\n--- HISTORICO FINANCEIRO DETALHADO ---");
                for (int i = 0; i < histFinData.size(); i++) {
                    System.out.printf("%s | %-15s | %-34s | Valor: %-11s | Imp: %-9s | Lucro: %-10s | Saldo: %s%n",
                            obterStringSeguro(histFinData, i, ""),
                            obterStringSeguro(histFinTipo, i, ""),
                            obterStringSeguro(histFinDescricao, i, ""),
                            FMT_MOEDA.format(obterDoubleSeguro(histFinValor, i, 0.0)),
                            FMT_MOEDA.format(obterDoubleSeguro(histFinImposto, i, 0.0)),
                            FMT_MOEDA.format(obterDoubleSeguro(histFinLucro, i, 0.0)),
                            FMT_MOEDA.format(obterDoubleSeguro(histFinSaldoApos, i, 0.0)));
                }
            }
        }
    }

    private static void moduloTI(Scanner sc) {
        if (!exigirAutoridade("TI", "TI")) return;
        while (true) {
            System.out.println("\n--- MODULO DE TI ---");
            System.out.println("1-Desbloquear login | 2-Ajustar autoridades | 3-Historico de acessos");
            System.out.println("4-Criar backup manual (.TXT) | 5-Olhar arquivos .TXT salvos | 6-Voltar");
            int op = lerInteiroFaixa(sc, "Escolha", 1, 6, false);
            if (op == Integer.MIN_VALUE || op == 6) return;

            if (op == 1) {
                String idF = lerIdValido(sc, "ID do funcionario bloqueado");
                if (idF == null) continue;
                int idx = funcId.indexOf(idF);
                if (idx == -1) {
                    reportarErro("FUNCIONARIO INEXISTENTE", "Funcionario nao encontrado.");
                    continue;
                }
                if ("GERENCIA".equalsIgnoreCase(obterStringSeguro(funcCargo, idx, "")) && !confirmarSenhaGerencia(sc)) {
                    continue;
                }
                definirBooleanSeguro(funcBloqueado, idx, false);
                definirIntSeguro(funcTentativas, idx, 0);
                salvarFuncionariosTxt();
                System.out.println("[OK] Login desbloqueado.");
            } else if (op == 2) {
                String idF = lerIdValido(sc, "ID do funcionario");
                if (idF == null) continue;
                int idx = funcId.indexOf(idF);
                if (idx == -1) {
                    reportarErro("FUNCIONARIO INEXISTENTE", "Funcionario nao encontrado.");
                    continue;
                }
                if ("GERENCIA".equalsIgnoreCase(obterStringSeguro(funcCargo, idx, "")) && !confirmarSenhaGerencia(sc)) {
                    continue;
                }
                if (!obterBooleanSeguro(funcAtivo, idx, false)) {
                    reportarErro("FUNCIONARIO INATIVO", "Funcionario desativado: autoridades nao ficam validas enquanto inativo.");
                    continue;
                }
                String novasAut = lerTextoObrigatorio(sc, "Autoridades (ex: RH,ESTOQUE,FINANCEIRO,CAIXA,TI)");
                if (novasAut != null) {
                    definirStringSeguro(funcAutoridades, idx, novasAut.toUpperCase());
                    salvarFuncionariosTxt();
                    System.out.println("[OK] Autoridades atualizadas.");
                }
            } else if (op == 3) {
                System.out.println("\n--- HISTORICO DE ACESSOS E UTILIZACAO ---");
                for (int i = 0; i < histAcData.size(); i++) {
                    System.out.printf("%s | Usuario: %-10s | Acao: %s%n",
                            obterStringSeguro(histAcData, i, ""),
                            obterStringSeguro(histAcUsuario, i, ""),
                            obterStringSeguro(histAcAcao, i, ""));
                }
            } else if (op == 4) {
                if (salvarBackupCompleto("BACKUP_MANUAL_TI")) {
                    System.out.println("[OK] Backup salvo em " + ARQ_BACKUP);
                }
            } else if (op == 5) {
                String nomeArq = lerNomeArquivoTxtValido(sc, "Nome do arquivo .TXT para consultar");
                if (nomeArq == null) continue;
                File arq = new File(nomeArq);
                if (!arq.exists()) {
                    reportarErro("ARQUIVO INEXISTENTE", "Arquivo nao encontrado no disco.");
                    continue;
                }
                System.out.println("\n--- CONTEUDO DE " + nomeArq + " ---");
                try (BufferedReader br = new BufferedReader(new FileReader(arq))) {
                    String linha;
                    while ((linha = br.readLine()) != null) {
                        System.out.println(linha);
                    }
                } catch (IOException e) {
                    reportarErro("ARQUIVO", "Erro ao ler arquivo: " + e.getMessage());
                }
            }
        }
    }

    // ==================== TELA INICIAL E MAIN ====================

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(SistemaGestaoConceitual::executarBackupAutomaticoEncerramento));
        inicializarSistemaComVerificacaoBackup();

        try (Scanner sc = new Scanner(System.in)) {
            while (true) {
                usuarioLogadoId = null;

                System.out.println("\n====================================================");
                System.out.println("      SISTEMA UNIFICADO DE GESTAO E VENDAS (INICIO)   ");
                System.out.println("====================================================");
                System.out.println("1 - Entrar como Cliente");
                System.out.println("2 - Entrar como Funcionario (Login)");
                System.out.println("3 - Explicacao das funcoes e excecoes");
                System.out.println("4 - Sair e salvar backup");

                int op = lerInteiroFaixa(sc, "Escolha uma opcao", 1, 4, false);
                if (op == Integer.MIN_VALUE) {
                    continue;
                }
                if (op == 1) {
                    fluxoCliente(sc);
                } else if (op == 2) {
                    if (loginFuncionario(sc)) {
                        menuFuncionario(sc);
                    }
                } else if (op == 3) {
                    exibirExplicacaoFuncoesEExcecoes();
                } else if (op == 4) {
                    salvarBackupCompleto("SAIDA_USUARIO");
                    backupExecutadoAoSair = true;
                    System.out.println("[OK] Backup salvo. Encerrando sistema.");
                    return;
                }
            }
        }
    }
}
