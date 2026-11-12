import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

/**
 * =============================================================================
 * SISTEMA PROTEI CREAT (L'ESTOQUE) - VERSÃO CORPORATIVA MASTER SECURITY & PAY
 * Governança RBAC, Modais de Detalhes, Pagamento e Sincronização Atômica.
 * =============================================================================
 */
public class prototo {

    /* =========================================================================
       [MÓDULO 1] MATRIZ FISCAL E TRIBUTÁRIA (RECEITA FEDERAL / 2026-2027)
       ========================================================================= */
    public static double aliquotaIcmsInterno2026 = 0.18;
    public static double aliquotaIcmsInterno2027 = 0.19; 

    public static double buscarAliquotaIcms(String ncm, String ufOrigem, String ufDestino, int anoExercicio) {
        double aliquotaBase = (anoExercicio == 2027) ? aliquotaIcmsInterno2027 : aliquotaIcmsInterno2026;
        if (ufOrigem.equalsIgnoreCase("SP") && ufDestino.equalsIgnoreCase("SP")) {
            return aliquotaBase; 
        } else {
            return 0.12; 
        }
    }

    /* =========================================================================
       [MÓDULO 2] UTILITÁRIOS, LOG DE AUDITORIA E SEGURANÇA
       ========================================================================= */
    public static String geradorSenhaAleatoriaSistema() {
        String caracteres = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$";
        Random random = new Random();
        StringBuilder senha = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            senha.append(caracteres.charAt(random.nextInt(caracteres.length())));
        }
        return senha.toString();
    }

    public static String formatarMoeda(double valor) {
        return String.format("R$ %.2f", valor);
    }

    public static void registrarLogAuditoria(List<String> logHistorico, String acao, String detalhes) {
        String registro = "[LOG AUDITORIA] " + acao + " -> " + detalhes;
        logHistorico.add(registro);
    }

    /* =========================================================================
       [MÓDULO 3] GERADORES DE MASSA DE DADOS (SINCRONIZADOS)
       ========================================================================= */
    public static void carregarMassaFuncionarios(List<String> funcId, List<String> funcNome, List<String> funcCpf, 
                                                  List<String> funcCargo, List<String> funcSenha, List<Double> funcSalario, 
                                                  List<Boolean> funcPago, List<String> funcStatus, List<String> funcMotivoInativo) {
        funcId.clear(); funcNome.clear(); funcCpf.clear(); funcCargo.clear(); 
        funcSenha.clear(); funcSalario.clear(); funcPago.clear(); funcStatus.clear(); funcMotivoInativo.clear();

        funcId.addAll(Arrays.asList("001", "002", "003", "004", "005", "006", "007", "008"));
        funcNome.addAll(Arrays.asList("ANA SILVA", "BRUNO LIMA", "CAMILA SOUZA", "MARCOS OLIVEIRA", "JULIA MENDES", "ROBERTO ALVES", "PATRICIA LIMA", "LUCAS SANTIAGO"));
        funcCpf.addAll(Arrays.asList("52998224725", "12345678909", "98765432100", "45678912300", "78912345600", "14725836900", "36925814700", "74185296300"));
        funcCargo.addAll(Arrays.asList("GERENTE", "OPERADOR", "FINANCEIRO", "OPERADOR", "GERENTE", "OPERADOR", "FINANCEIRO", "OPERADOR"));
        funcSenha.addAll(Arrays.asList("admin123", "op456", "fin789", "op999", "ger888", "op111", "fin222", "op333"));
        funcSalario.addAll(Arrays.asList(5500.00, 2200.00, 4800.00, 2200.00, 5500.00, 2200.00, 4800.00, 2200.00));
        funcPago.addAll(Arrays.asList(false, false, false, true, true, false, false, false));
        funcStatus.addAll(Arrays.asList("ATIVO", "ATIVO", "ATIVO", "DEMITIDO", "DEMITIDO", "ATIVO", "ATIVO", "ATIVO"));
        funcMotivoInativo.addAll(Arrays.asList("NENHUM", "NENHUM", "NENHUM", "Corte de custos", "Pedido de demissão", "NENHUM", "NENHUM", "NENHUM"));
    }

    public static void carregarMassaFornecedores(List<String> fornNome, List<String> fornCnpj, List<String> fornUf, 
                                                 List<String> fornProdutoFornecido, List<Double> fornPrecoPago, 
                                                 List<Double> fornLucroPretendidoPct, List<Boolean> fornPago) {
        fornNome.clear(); fornCnpj.clear(); fornUf.clear(); fornProdutoFornecido.clear(); 
        fornPrecoPago.clear(); fornLucroPretendidoPct.clear(); fornPago.clear();

        fornNome.addAll(Arrays.asList("SUPLEMAX LTDA", "VITAFIT DISTRIBUIDORA", "GLOBAL NUTRI", "FARMACOMERCIAL BRASIL", "BIOHEDGER", "ATLAS SUPLEMENTOS"));
        fornCnpj.addAll(Arrays.asList("12345678000190", "98765432000110", "45678912000155", "78912345000188", "32165498000177", "65498732000144"));
        fornUf.addAll(Arrays.asList("SP", "RJ", "MG", "PR", "RS", "SC"));
        fornProdutoFornecido.addAll(Arrays.asList("WHEY PROTEIN", "CREATINA", "BCAA 100G", "VITAMINAS", "GLUTAMINA", "BARRA PROTEICA"));
        fornPrecoPago.addAll(Arrays.asList(4500.00, 3200.00, 2800.00, 1900.00, 3100.00, 1500.00));
        fornLucroPretendidoPct.addAll(Arrays.asList(40.0, 35.0, 40.0, 45.0, 35.0, 40.0));
        fornPago.addAll(Arrays.asList(false, false, false, false, false, false));
    }

    public static void carregarMassaClientes(List<String> clienteCpf, List<String> clienteNome) {
        clienteCpf.clear(); clienteNome.clear();
        clienteCpf.addAll(Arrays.asList("11144477735", "22255588846", "33366699957", "44477700068", "55588811179", "66699922280"));
        clienteNome.addAll(Arrays.asList("JOAO PEREIRA", "MARIA ALMEIDA", "CARLOS EDUARDO", "BEATRIZ SOUZA", "LUCAS MARTINS", "JULIANA ROCHA"));
    }

    public static void carregarMassaDadosPadrao(List<String> prodNome, List<String> prodNcm, List<Double> prodPrecos, 
                                                List<Integer> prodEstoque, List<String> clienteCpf, List<String> clienteNome,
                                                List<String> transacaoTipo, List<String> transacaoOrigemDestino, 
                                                List<String> transacaoDetalhes, List<Double> transacaoValor, 
                                                List<Double> caixaGlobalRef, List<Double> receitaVendasRef, List<String> logHistorico) {
        System.out.println("\n[SISTEMA]: Carregando massa PADRÃO e transações...");

        prodNome.clear(); prodNcm.clear(); prodPrecos.clear(); prodEstoque.clear();
        String[] categoriasProd = {"WHEY", "CREATINA", "BCAA", "VITAMINA", "PRE-WORKOUT", "GLUTAMINA"};
        for (int p = 1; p <= 55; p++) {
            String cat = categoriasProd[(p - 1) % categoriasProd.length];
            prodNome.add(cat + " PADRÃO " + p);
            prodNcm.add("2106.90." + (10 + (p % 89)));
            prodPrecos.add(35.0 + (p * 2.0));
            prodEstoque.add(40 + (p % 40));
        }

        transacaoTipo.clear(); transacaoOrigemDestino.clear(); transacaoDetalhes.clear(); transacaoValor.clear();
        Random gerador = new Random(777);
        double receitaSimulada = 0.0;

        for (int t = 1; t <= 100; t++) {
            int prodIndex = gerador.nextInt(prodNome.size());
            int qtdVendida = 1 + gerador.nextInt(4);
            double val = prodPrecos.get(prodIndex) * qtdVendida;
            String cliente = clienteNome.get(gerador.nextInt(clienteNome.size()));

            transacaoTipo.add("VENDA");
            transacaoOrigemDestino.add("Cliente: " + cliente + " | Vendedor: CAIXA OFICIAL");
            transacaoDetalhes.add(qtdVendida + "x " + prodNome.get(prodIndex));
            transacaoValor.add(val);
            receitaSimulada += val;
        }

        transacaoTipo.add("COMPRA / ABASTECIMENTO");
        transacaoOrigemDestino.add("Fornecedor: SUPLEMAX LTDA");
        transacaoDetalhes.add("Reposição de Lote WHEY PROTEIN (50 unidades)");
        transacaoValor.add(4500.00);

        transacaoTipo.add("PAGAMENTO");
        transacaoOrigemDestino.add("Funcionário: ANA SILVA (GERENTE)");
        transacaoDetalhes.add("Quitação de Salário Mensal");
        transacaoValor.add(5500.00);

        receitaVendasRef.set(0, receitaSimulada);
        caixaGlobalRef.set(0, caixaGlobalRef.get(0) + receitaSimulada - 10000.00);
        registrarLogAuditoria(logHistorico, "SISTEMA", "Massa PADRÃO carregada.");
        System.out.println("✅ [Sucesso]: Base padrão ativa!\n");
    }

    public static void carregarMassaDadosRandom(List<String> prodNome, List<String> prodNcm, List<Double> prodPrecos, 
                                                List<Integer> prodEstoque, List<String> clienteCpf, List<String> clienteNome,
                                                List<String> transacaoTipo, List<String> transacaoOrigemDestino, 
                                                List<String> transacaoDetalhes, List<Double> transacaoValor, 
                                                List<Double> caixaGlobalRef, List<Double> receitaVendasRef, List<String> logHistorico) {
        System.out.println("\n[SISTEMA]: Gerando massa RANDÔMICA...");

        prodNome.clear(); prodNcm.clear(); prodPrecos.clear(); prodEstoque.clear();
        Random rand = new Random();
        String[] categoriasProd = {"WHEY", "CREATINA", "BCAA", "VITAMINA", "PRE-WORKOUT", "GLUTAMINA", "BARRA", "OMEGA"};
        
        int totalProdutos = 50 + rand.nextInt(21);
        for (int p = 1; p <= totalProdutos; p++) {
            String cat = categoriasProd[rand.nextInt(categoriasProd.length)];
            prodNome.add(cat + " RANDOM " + p);
            prodNcm.add("2106.90." + (10 + rand.nextInt(89)));
            prodPrecos.add(30.0 + (rand.nextDouble() * 150.0));
            prodEstoque.add(10 + rand.nextInt(90));
        }

        transacaoTipo.clear(); transacaoOrigemDestino.clear(); transacaoDetalhes.clear(); transacaoValor.clear();
        double receitaSimulada = 0.0;
        int totalTransacoes = 60 + rand.nextInt(40);
        
        for (int t = 1; t <= totalTransacoes; t++) {
            int prodIndex = rand.nextInt(prodNome.size());
            int qtdVendida = 1 + rand.nextInt(5);
            double val = prodPrecos.get(prodIndex) * qtdVendida;
            String cliente = clienteNome.get(rand.nextInt(clienteNome.size()));

            transacaoTipo.add("VENDA");
            transacaoOrigemDestino.add("Cliente: " + cliente + " | Vendedor: AUTÔNOMO");
            transacaoDetalhes.add(qtdVendida + "x " + prodNome.get(prodIndex));
            transacaoValor.add(val);
            receitaSimulada += val;
        }

        receitaVendasRef.set(0, receitaSimulada);
        caixaGlobalRef.set(0, caixaGlobalRef.get(0) + receitaSimulada);
        registrarLogAuditoria(logHistorico, "SISTEMA", "Massa RANDOM gerada.");
        System.out.println("✅ [Sucesso]: Base randômica ativada!\n");
    }

    /* =========================================================================
       [MÓDULO 4] VALIDAÇÃO DE ALÇADA E AUTORIDADE (RBAC)
       ========================================================================= */
    public static boolean verificarAutoridadeGerencial(String cargoLogado) {
        if (cargoLogado.equalsIgnoreCase("GERENTE") || cargoLogado.equalsIgnoreCase("FINANCEIRO")) {
            return true;
        }
        System.out.println("❌ [BLOQUEIO DE ALÇADA]: Acesso negado! Apenas GERENTES ou FINANCEIRO podem realizar alterações.\n");
        return false;
    }

    /* =========================================================================
       [MÓDULO 5] BACKUP E SEGURANÇA
       ========================================================================= */
    public static void gerarRollbackAutomatico(List<String> funcId, List<String> funcNome, List<String> funcCpf, 
                                              List<String> funcCargo, List<String> funcSenha, List<Double> funcSalario, 
                                              List<Boolean> funcPago, List<String> funcStatus, List<String> funcMotivoInativo,
                                              List<String> prodNome, List<String> prodNcm, List<Double> prodPrecos, List<Integer> prodEstoque, 
                                              List<Double> caixaGlobal) {
        String arquivoRollback = "protei_creat_rollback_auto.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(arquivoRollback))) {
            writer.write("#--- REDUNDÂNCIA PREVENTIVA (ROLLBACK) ---#\n");
            writer.write("[FUNCIONARIOS_INICIO]\n");
            for (int i = 0; i < funcId.size(); i++) {
                writer.write(funcId.get(i) + ";" + funcNome.get(i) + ";" + funcCpf.get(i) + ";" + 
                             funcCargo.get(i) + ";" + funcSenha.get(i) + ";" + funcSalario.get(i) + ";" + 
                             funcPago.get(i) + ";" + funcStatus.get(i) + ";" + funcMotivoInativo.get(i) + "\n");
            }
            writer.write("[FUNCIONARIOS_FIM]\n");
            writer.write("[PRODUTOS_INICIO]\n");
            for (int i = 0; i < prodNome.size(); i++) {
                if (prodNome.get(i) != null) {
                    writer.write(prodNome.get(i) + ";" + prodNcm.get(i) + ";" + prodPrecos.get(i) + ";" + prodEstoque.get(i) + "\n");
                }
            }
            writer.write("[PRODUTOS_FIM]\n");
            writer.write("[CAIXA_INICIO]\n" + caixaGlobal.get(0) + "\n[CAIXA_FIM]\n");
        } catch (IOException e) {}
    }

    public static void realizarBackupTerminal(List<String> funcId, List<String> funcNome, List<String> funcCpf, 
                                             List<String> funcCargo, List<String> funcSenha, List<Double> funcSalario, 
                                             List<Boolean> funcPago, List<String> funcStatus, List<String> funcMotivoInativo,
                                             List<String> prodNome, List<String> prodNcm, List<Double> prodPrecos, 
                                             List<Integer> prodEstoque, List<Double> caixaGlobal, List<String> transacaoTipo, 
                                             List<String> logHistorico, Scanner scanner, String usuarioLogado, String senhaLogado) {
        System.out.println("\n================================================================");
        System.out.println("            TERMINAL: EXPORTAR BACKUP OFICIAL                   ");
        System.out.println("================================================================");
        System.out.print("🔒 Confirme sua senha (" + usuarioLogado + ") [0 para cancelar]: ");
        String senhaInput = scanner.nextLine().trim();
        if (!senhaInput.equals(senhaLogado)) {
            System.out.println("❌ Senha incorreta.\n");
            return;
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("protei_creat_backup_oficial.txt"))) {
            writer.write("#--- BACKUP OFICIAL PROTEI CREAT ---#\n");
            writer.write("[CAIXA_INICIO]\n" + caixaGlobal.get(0) + "\n[CAIXA_FIM]\n");
            registrarLogAuditoria(logHistorico, usuarioLogado, "Executou backup oficial");
            System.out.println("✅ Backup gerado com sucesso!\n");
        } catch (IOException e) {
            System.out.println("❌ Erro ao gerar backup.\n");
        }
    }

    public static void restaurarBackupTerminal(List<String> funcId, List<String> funcNome, List<String> funcCpf, 
                                               List<String> funcCargo, List<String> funcSenha, List<Double> funcSalario, 
                                               List<Boolean> funcPago, List<String> funcStatus, List<String> funcMotivoInativo,
                                               List<String> prodNome, List<String> prodNcm, List<Double> prodPrecos, 
                                               List<Integer> prodEstoque, List<Double> caixaGlobal, List<String> logHistorico, 
                                               Scanner scanner, String cargoLogado, String usuarioLogado, String senhaLogado) {
        if (!cargoLogado.equalsIgnoreCase("GERENTE")) {
            System.out.println("❌ Acesso negado. Apenas Gerentes podem restaurar backups.\n");
            return;
        }
        System.out.print("🔒 Senha de Gerente: ");
        if (!scanner.nextLine().trim().equals(senhaLogado)) {
            System.out.println("❌ Senha incorreta.\n");
            return;
        }
        System.out.println("✅ Simulação de restauração executada com sucesso!\n");
    }

    /* =========================================================================
       [MÓDULO 6] VALIDAÇÕES DE INPUTS E DOCUMENTOS
       ========================================================================= */
    public static String lerTextoApenasLetras(Scanner scanner, String mensagem) {
        String entrada = "";
        while (true) {
            System.out.print(mensagem);
            entrada = scanner.nextLine().trim();
            if (entrada.equalsIgnoreCase("0") || entrada.equalsIgnoreCase("SAIR")) return "CANCELAR";
            if (!entrada.isEmpty() && entrada.matches("^[a-zA-ZáàâãéèêíïóôõúüçÁÀÂÃÉÈÊÍÏÓÔÕÚÜÇ\\s]+$")) break;
            System.out.println("❌ [Erro]: Texto inválido.\n");
        }
        return entrada.toUpperCase();
    }

    public static String lerUfValida(Scanner scanner, String mensagem) {
        List<String> ufs = Arrays.asList("AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO");
        while (true) {
            System.out.print(mensagem);
            String uf = scanner.nextLine().trim().toUpperCase();
            if (uf.equals("0") || uf.equalsIgnoreCase("SAIR")) return "CANCELAR";
            if (ufs.contains(uf)) return uf;
            System.out.println("❌ [Erro]: UF inválida.\n");
        }
    }

    public static int lerInteiroValido(Scanner scanner, String mensagem) {
        while (true) {
            System.out.print(mensagem);
            String entrada = scanner.nextLine().trim();
            if (entrada.equalsIgnoreCase("0") || entrada.equalsIgnoreCase("SAIR")) return -999;
            if (entrada.matches("^[0-9]+$")) return Integer.parseInt(entrada);
            System.out.println("❌ [Erro]: Digite números inteiros.\n");
        }
    }

    public static double lerDoubleValido(Scanner scanner, String mensagem) {
        while (true) {
            System.out.print(mensagem);
            String entrada = scanner.nextLine().trim().replace(",", ".");
            if (entrada.equalsIgnoreCase("SAIR")) return -999.0;
            try {
                double val = Double.parseDouble(entrada);
                if (Double.isFinite(val) && val >= 0) return val;
            } catch (Exception e) {}
            System.out.println("❌ [Erro]: Valor inválido.\n");
        }
    }

    public static String lerCpfValido(Scanner scanner) {
        while (true) {
            System.out.print("Digite o CPF (11 números) [0 para cancelar]: ");
            String entrada = scanner.nextLine().trim();
            if (entrada.equals("0") || entrada.equalsIgnoreCase("SAIR")) return "CANCELAR";
            String cpf = entrada.replaceAll("[^0-9]", "");
            if (cpf.length() == 11) return cpf;
            System.out.println("❌ [Erro]: CPF deve conter 11 dígitos.\n");
        }
    }

    public static String lerCnpjValido(Scanner scanner) {
        while (true) {
            System.out.print("Digite o CNPJ (14 números) [0 para cancelar]: ");
            String entrada = scanner.nextLine().trim();
            if (entrada.equals("0") || entrada.equalsIgnoreCase("SAIR")) return "CANCELAR";
            String cnpj = entrada.replaceAll("[^0-9]", "");
            if (cnpj.length() == 14) return cnpj;
            System.out.println("❌ [Erro]: CNPJ deve conter 14 dígitos.\n");
        }
    }

    public static double obterSalarioPadraoCargo(String cargo) {
        if (cargo.equalsIgnoreCase("GERENTE")) return 5500.00;
        if (cargo.equalsIgnoreCase("FINANCEIRO")) return 4800.00;
        return 2200.00; 
    }

    public static void exibirCabecalho() {
        System.out.println("\n================================================================");
        System.out.println("            Bem vindo ao Protei Creat                           ");
        System.out.println("     SISTEMA DE GESTÃO TRIBUTÁRIA E ESTOQUE - L'ESTOQUE         ");
        System.out.println("================================================================");
    }

    /* =========================================================================
       [MÓDULO 7] PAINEL DE GESTÃO DE FUNCIONÁRIOS (COM MODAL, DETALHES E PAGAMENTO)
       ========================================================================= */
    public static void menuGerenciarFuncionarios(Scanner scanner, List<String> funcId, List<String> funcNome, List<String> funcCpf, 
                                                   List<String> funcCargo, List<String> funcSenha, List<Double> funcSalario, 
                                                   List<Boolean> funcPago, List<String> funcStatus, List<String> funcMotivoInativo, 
                                                   List<String> logHistorico, String usuarioLogado, String cargoLogado,
                                                   List<String> prodNome, List<String> prodNcm, List<Double> prodPrecos, List<Integer> prodEstoque, 
                                                   List<Double> caixaGlobal, List<String> transacaoTipo, List<String> transacaoOrigemDestino, 
                                                   List<String> transacaoDetalhes, List<Double> transacaoValor) {
        boolean rodando = true;
        while (rodando) {
            System.out.println("\n================================================================");
            System.out.println("        PAINEL: GESTÃO DE AUTORIDADES E FUNCIONÁRIOS            ");
            System.out.println("================================================================");
            System.out.println("[1] Listar funcionários com Paginação (25/pág) + Modal de Detalhes/Pagamento");
            System.out.println("[2] Cadastrar novo funcionário");
            System.out.println("[3] Voltar");
            System.out.println("================================================================");

            int op = lerInteiroValido(scanner, "Opção: ");
            if (op == 3 || op == -999) rodando = false;
            else if (op == 1) {
                int pagina = 1;
                int porPagina = 25;
                boolean navegandoFunc = true;
                while (navegandoFunc) {
                    int totalPaginas = Math.max(1, (int) Math.ceil((double) funcNome.size() / porPagina));
                    if (pagina > totalPaginas) pagina = totalPaginas;

                    System.out.println("\n--- RELATÓRIO DE FUNCIONÁRIOS (Página " + pagina + "/" + totalPaginas + ") ---");
                    int inicio = (pagina - 1) * porPagina;
                    int fim = Math.min(inicio + porPagina, funcNome.size());

                    for (int i = inicio; i < fim; i++) {
                        String statusPagamento = funcPago.get(i) ? "PAGO ✅" : "PENDENTE ⚠️";
                        System.out.printf("[%d] ID: %s | Nome: %s | Cargo: %s | Status: %s | Salário: %s [%s]\n", 
                            (i + 1), funcId.get(i), funcNome.get(i), funcCargo.get(i), funcStatus.get(i), formatarMoeda(funcSalario.get(i)), statusPagamento);
                    }
                    System.out.println("----------------------------------------------------------------");
                    System.out.println("[N] Próxima | [P] Anterior | [NÚMERO DO ITEM] Abrir Modal | [0] Voltar");
                    System.out.print("Comando: ");
                    String cmd = scanner.nextLine().trim().toUpperCase();

                    if (cmd.equals("N") && pagina < totalPaginas) pagina++;
                    else if (cmd.equals("P") && pagina > 1) pagina--;
                    else if (cmd.equals("0") || cmd.equals("SAIR")) navegandoFunc = false;
                    else if (cmd.matches("^[0-9]+$")) {
                        int idx = Integer.parseInt(cmd) - 1;
                        if (idx >= 0 && idx < funcNome.size()) {
                            boolean emModalFunc = true;
                            while (emModalFunc) {
                                System.out.println("\n┌────────────────────────────────────────────────────────┐");
                                System.out.println("│               [MODAL DE DETALHES E GESTÃO]             │");
                                System.out.println("├────────────────────────────────────────────────────────┤");
                                System.out.printf("│ Nome: %s | ID: %s | CPF: %s\n", funcNome.get(idx), funcId.get(idx), funcCpf.get(idx));
                                System.out.printf("│ Cargo: %s | Status: %s\n", funcCargo.get(idx), funcStatus.get(idx));
                                System.out.printf("│ Salário: %s | Pagamento: %s\n", formatarMoeda(funcSalario.get(idx)), (funcPago.get(idx) ? "PAGO" : "PENDENTE"));
                                if (!funcStatus.get(idx).equalsIgnoreCase("ATIVO")) {
                                    System.out.printf("│ Motivo Inativo: %s\n", funcMotivoInativo.get(idx));
                                }
                                System.out.println("├────────────────────────────────────────────────────────┤");
                                System.out.println("│ [1] Ver Ficha de Detalhes Completa                     │");
                                System.out.println("│ [2] Efetuar Pagamento de Salário (Caso pendente)      │");
                                System.out.println("│ [3] Editar Nome                                        │");
                                System.out.println("│ [4] Demitir / Inativar Funcionário                     │");
                                System.out.println("│ [0] Fechar Modal                                       │");
                                System.out.println("└────────────────────────────────────────────────────────┘");
                                int acaoModal = lerInteiroValido(scanner, "Ação no Modal: ");
                                
                                if (acaoModal == 1) {
                                    System.out.println("\n--- FICHA TÉCNICA DETALHADA ---");
                                    System.out.println("• Colaborador : " + funcNome.get(idx));
                                    System.out.println("• Registro ID : " + funcId.get(idx));
                                    System.out.println("• Documento   : " + funcCpf.get(idx));
                                    System.out.println("• Alçada      : " + funcCargo.get(idx));
                                    System.out.println("• Vencimento  : Mensal (Dia 05)");
                                    System.out.println("----------------------------------------------------------------\n");
                                } else if (acaoModal == 2) {
                                    if (!verificarAutoridadeGerencial(cargoLogado)) continue;
                                    if (funcPago.get(idx)) {
                                        System.out.println("⚠️ Este funcionário já consta como PAGO!\n");
                                    } else {
                                        double valSalario = funcSalario.get(idx);
                                        caixaGlobal.set(0, caixaGlobal.get(0) - valSalario);
                                        funcPago.set(idx, true);

                                        transacaoTipo.add("PAGAMENTO");
                                        transacaoOrigemDestino.add("Funcionário: " + funcNome.get(idx));
                                        transacaoDetalhes.add("Quitação de Salário Mensal");
                                        transacaoValor.add(valSalario);

                                        registrarLogAuditoria(logHistorico, usuarioLogado, "Pagamento efetuado para " + funcNome.get(idx));
                                        System.out.println("✅ Pagamento efetuado com sucesso! Caixa e transações sincronizadas.\n");
                                    }
                                } else if (acaoModal == 3) {
                                    if (!verificarAutoridadeGerencial(cargoLogado)) continue;
                                    String novoNome = lerTextoApenasLetras(scanner, "Novo Nome: ");
                                    funcNome.set(idx, novoNome);
                                    System.out.println("✅ Nome atualizado com sucesso!\n");
                                } else if (acaoModal == 4) {
                                    if (!verificarAutoridadeGerencial(cargoLogado)) continue;
                                    System.out.print("Motivo do desligamento: ");
                                    String motivo = scanner.nextLine().trim();
                                    funcStatus.set(idx, "DEMITIDO");
                                    funcMotivoInativo.set(idx, motivo.isEmpty() ? "NÃO INFORMADO" : motivo);
                                    System.out.println("✅ Funcionário inativado com sucesso!\n");
                                    emModalFunc = false;
                                } else if (acaoModal == 0 || acaoModal == -999) {
                                    emModalFunc = false;
                                }
                            }
                        }
                    }
                }
            } else if (op == 2) {
                if (!verificarAutoridadeGerencial(cargoLogado)) continue;
                gerarRollbackAutomatico(funcId, funcNome, funcCpf, funcCargo, funcSenha, funcSalario, funcPago, funcStatus, funcMotivoInativo, prodNome, prodNcm, prodPrecos, prodEstoque, caixaGlobal);
                String novoId = "";
                while (true) {
                    System.out.print("ID Funcional (3 dígitos) [0 para sair]: ");
                    novoId = scanner.nextLine().trim().toUpperCase();
                    if (novoId.equals("0") || novoId.equalsIgnoreCase("SAIR")) break;
                    if (novoId.length() == 3 && !funcId.contains(novoId)) break;
                    System.out.println("❌ ID inválido ou em uso.\n");
                }
                if (novoId.equals("0") || novoId.equalsIgnoreCase("SAIR")) continue;

                String nome = lerTextoApenasLetras(scanner, "Nome completo: ");
                String cpf = lerCpfValido(scanner);
                int cOpt = lerInteiroValido(scanner, "Cargo [1 - Gerente | 2 - Operador | 3 - Financeiro]: ");
                String cargo = (cOpt == 1) ? "GERENTE" : (cOpt == 3 ? "FINANCEIRO" : "OPERADOR");

                funcId.add(novoId); funcNome.add(nome); funcCpf.add(cpf); funcCargo.add(cargo);
                funcSenha.add(geradorSenhaAleatoriaSistema()); funcSalario.add(obterSalarioPadraoCargo(cargo));
                funcPago.add(false); funcStatus.add("ATIVO"); funcMotivoInativo.add("NENHUM");
                registrarLogAuditoria(logHistorico, usuarioLogado, "Contratação: " + nome);
                System.out.println("✅ Cadastrado com sucesso!\n");
            }
        }
    }

    public static void menuGerenciarFornecedores(Scanner scanner, List<String> fornNome, List<String> fornCnpj, List<String> fornUf, 
                                                   List<String> fornProdutoFornecido, List<Double> fornPrecoPago, 
                                                   List<Double> fornLucroPretendidoPct, List<Boolean> fornPago, List<String> logHistorico, String usuarioLogado, String cargoLogado,
                                                   List<String> prodNome, List<Integer> prodEstoque, List<Double> caixaGlobal, 
                                                   List<String> transacaoTipo, List<String> transacaoOrigemDestino, 
                                                   List<String> transacaoDetalhes, List<Double> transacaoValor) {
        boolean rodando = true;
        while (rodando) {
            System.out.println("\n================================================================");
            System.out.println("                    PAINEL: GESTÃO DE FORNECEDORES              ");
            System.out.println("================================================================");
            System.out.println("[1] Listar fornecedores com Modal (Detalhes, Pagamento e Edição)");
            System.out.println("[2] Cadastrar novo fornecedor e realizar Compra Sincronizada");
            System.out.println("[3] Voltar");
            System.out.println("================================================================");
            int op = lerInteiroValido(scanner, "Opção: ");
            if (op == 3 || op == -999) rodando = false;
            else if (op == 1) {
                gerenciarListaPaginadaFornecedoresModal(fornNome, fornCnpj, fornUf, fornProdutoFornecido, fornPrecoPago, fornPago, scanner, cargoLogado, caixaGlobal, transacaoTipo, transacaoOrigemDestino, transacaoDetalhes, transacaoValor, logHistorico, usuarioLogado);
            } else if (op == 2) {
                if (!verificarAutoridadeGerencial(cargoLogado)) continue;
                String nome = lerTextoApenasLetras(scanner, "Razão Social: ");
                String cnpj = lerCnpjValido(scanner);
                String uf = lerUfValida(scanner, "UF: ");
                System.out.print("Produto principal fornecido: ");
                String prod = scanner.nextLine().trim().toUpperCase();
                double custo = lerDoubleValido(scanner, "Custo unitário da compra (R$): ");
                int qtdAbastecimento = lerInteiroValido(scanner, "Quantidade para abastecer no estoque: ");

                fornNome.add(nome); fornCnpj.add(cnpj); fornUf.add(uf);
                fornProdutoFornecido.add(prod); fornPrecoPago.add(custo); fornLucroPretendidoPct.add(40.0); fornPago.add(true);

                double valorTotalCompra = custo * qtdAbastecimento;
                caixaGlobal.set(0, caixaGlobal.get(0) - valorTotalCompra);

                int idxProd = prodNome.indexOf(prod);
                if (idxProd != -1) {
                    prodEstoque.set(idxProd, prodEstoque.get(idxProd) + qtdAbastecimento);
                } else {
                    prodNome.add(prod);
                    prodEstoque.add(qtdAbastecimento);
                }

                transacaoTipo.add("COMPRA / ABASTECIMENTO");
                transacaoOrigemDestino.add("Fornecedor: " + nome + " (UF: " + uf + ")");
                transacaoDetalhes.add("Abastecimento: " + qtdAbastecimento + "x " + prod);
                transacaoValor.add(valorTotalCompra);

                registrarLogAuditoria(logHistorico, usuarioLogado, "Nova compra sincronizada com " + nome);
                System.out.println("✅ Fornecedor cadastrado e estoque/caixa atualizados!\n");
            }
        }
    }

    public static void gerenciarListaPaginadaFornecedoresModal(List<String> fNome, List<String> fCnpj, List<String> fUf, List<String> fProd, List<Double> fPreco, List<Boolean> fPago, Scanner scanner, String cargoLogado, List<Double> caixaGlobal, List<String> tTipo, List<String> tOrigem, List<String> tDetalhes, List<String> tValor, List<String> logHistorico, String usuarioLogado) {
        int pagina = 1;
        int porPagina = 25;
        boolean rodando = true;
        while (rodando) {
            int totalPaginas = Math.max(1, (int) Math.ceil((double) fNome.size() / porPagina));
            if (pagina > totalPaginas) pagina = totalPaginas;

            System.out.println("\n=== LISTA DE FORNECEDORES (Página " + pagina + "/" + totalPaginas + ") ===");
            int inicio = (pagina - 1) * porPagina;
            int fim = Math.min(inicio + porPagina, fNome.size());

            for (int i = inicio; i < fim; i++) {
                String statusPagamento = fPago.get(i) ? "QUITADO ✅" : "PENDENTE ⚠️";
                System.out.printf("[%d] %s | CNPJ: %s | UF: %s | [%s]\n", (i + 1), fNome.get(i), fCnpj.get(i), fUf.get(i), statusPagamento);
            }
            System.out.println("----------------------------------------------------------------");
            System.out.println("[N] Próxima | [P] Anterior | [NÚMERO] Abrir Modal | [0] Voltar");
            System.out.print("Escolha: ");
            String op = scanner.nextLine().trim().toUpperCase();

            if (op.equals("N") && pagina < totalPaginas) pagina++;
            else if (op.equals("P") && pagina > 1) pagina--;
            else if (op.equals("0") || op.equals("SAIR")) rodando = false;
            else if (op.matches("^[0-9]+$")) {
                int idx = Integer.parseInt(op) - 1;
                if (idx >= 0 && idx < fNome.size()) {
                    boolean emModal = true;
                    while (emModal) {
                        System.out.println("\n┌────────────────────────────────────────────────────────┐");
                        System.out.println("│               [MODAL DE FORNECEDOR E PAGAMENTO]        │");
                        System.out.println("├────────────────────────────────────────────────────────┤");
                        System.out.printf("│ Fornecedor: %s\n", fNome.get(idx));
                        System.out.printf("│ CNPJ: %s | UF: %s | Produto: %s\n", fCnpj.get(idx), fUf.get(idx), fProd.get(idx));
                        System.out.printf("│ Status Financeiro: %s\n", (fPago.get(idx) ? "QUITADO" : "PENDENTE DE PAGAMENTO"));
                        System.out.println("├────────────────────────────────────────────────────────┤");
                        System.out.println("│ [1] Ver Detalhes Completos                             │");
                        System.out.println("│ [2] Pagar Fatura / Abastecimento Pendente              │");
                        System.out.println("│ [3] Editar Razão Social                                │");
                        System.out.println("│ [4] Excluir Fornecedor                                 │");
                        System.out.println("│ [0] Fechar Modal                                       │");
                        System.out.println("└────────────────────────────────────────────────────────┘");
                        int acaoModal = lerInteiroValido(scanner, "Ação: ");
                        if (acaoModal == 1) {
                            System.out.println("\n--- DETALHES DO FORNECEDOR ---");
                            System.out.println("• Empresa : " + fNome.get(idx));
                            System.out.println("• CNPJ    : " + fCnpj.get(idx));
                            System.out.println("• Origem  : Estado de " + fUf.get(idx));
                            System.out.println("• Insumo  : " + fProd.get(idx));
                            System.out.println("----------------------------------------------------------------\n");
                        } else if (acaoModal == 2) {
                            if (!verificarAutoridadeGerencial(cargoLogado)) continue;
                            if (fPago.get(idx)) {
                                System.out.println("⚠️ Fatura já quitada!\n");
                            } else {
                                double valorFatura = fPrecoPago.get(idx);
                                caixaGlobal.set(0, caixaGlobal.get(0) - valorFatura);
                                fPago.set(idx, true);

                                tTipo.add("PAGAMENTO");
                                tOrigem.add("Fornecedor: " + fNome.get(idx));
                                tDetalhes.add("Quitação de Fatura de Insumos");
                                tValor.add(valorFatura);

                                registrarLogAuditoria(logHistorico, usuarioLogado, "Pagamento quitado para fornecedor " + fNome.get(idx));
                                System.out.println("✅ Fatura paga e caixa sincronizado com sucesso!\n");
                            }
                        } else if (acaoModal == 3) {
                            if (!verificarAutoridadeGerencial(cargoLogado)) continue;
                            String novoNome = lerTextoApenasLetras(scanner, "Nova Razão Social: ");
                            fNome.set(idx, novoNome);
                            System.out.println("✅ Atualizado com sucesso!\n");
                        } else if (acaoModal == 4) {
                            if (!verificarAutoridadeGerencial(cargoLogado)) continue;
                            fNome.remove(idx); fCnpj.remove(idx); fUf.remove(idx); fProd.remove(idx); fPrecoPago.remove(idx); fPago.remove(idx);
                            System.out.println("🗑️ Fornecedor excluído!\n");
                            emModal = false;
                        } else if (acaoModal == 0 || acaoModal == -999) {
                            emModal = false;
                        }
                    }
                }
            }
        }
    }

    public static void menuFinanceiroEOperacional(Scanner scanner, List<String> prodNome, List<String> prodNcm, List<Double> prodPrecos, 
                                                   List<Integer> prodEstoque, List<Double> caixaGlobalRef, List<Double> gastosEstoqueRef,
                                                   List<Double> receitaVendasRef, List<Double> cmvRef,
                                                   List<Double> salariosPagosRef, List<Double> fornecedoresPagosRef,
                                                   List<Double> impostosPendentesRef, List<Boolean> funcPago, List<Boolean> fornPago,
                                                   List<Double> funcSalario, List<Double> fornPrecoPago, int[] anoFiscalRef,
                                                   List<String> transacaoTipo, List<String> transacaoOrigemDestino, 
                                                   List<String> transacaoDetalhes, List<Double> transacaoValor,
                                                   List<String> clienteCpf, List<String> clienteNome,
                                                   List<String> fornNome, List<String> fornCnpj, List<String> fornUf, List<String> fornProdutoFornecido, 
                                                   List<Double> fornLucroPretendidoPct, List<String> logHistorico, String usuarioLogado, String cargoLogado, String senhaLogado,
                                                   List<String> funcId, List<String> funcNome, List<String> funcCpf, List<String> funcCargo, 
                                                   List<String> funcSenha, List<String> funcStatus, List<String> funcMotivoInativo) {
        boolean rodando = true;
        while (rodando) {
            System.out.println("\n================================================================");
            System.out.println("      PAINEL: FINANCEIRO, ESTOQUE E TERMINAL (" + cargoLogado + ")      ");
            System.out.println("================================================================");
            System.out.println("[1] Listar catálogo de produtos (Paginado + Modal de Preço)");
            System.out.println("[2] Cadastrar novo produto");
            System.out.println("[3] Abrir Central de Relatórios e Transações Sincronizadas");
            System.out.println("[4] [TERMINAL] Gerar Backup Oficial");
            System.out.println("[5] [TERMINAL] Restaurar Backup");
            System.out.println("[6] Voltar");
            System.out.println("================================================================");

            int op = lerInteiroValido(scanner, "Opção: ");
            if (op == 6 || op == -999) rodando = false;
            else if (op == 1) {
                int pagina = 1;
                int porPagina = 25;
                boolean navegandoProd = true;
                while (navegandoProd) {
                    int totalPaginas = Math.max(1, (int) Math.ceil((double) prodNome.size() / porPagina));
                    if (pagina > totalPaginas) pagina = totalPaginas;

                    System.out.println("\n--- ESTOQUE ATUALIZADO (Página " + pagina + "/" + totalPaginas + ") ---");
                    int inicio = (pagina - 1) * porPagina;
                    int fim = Math.min(inicio + porPagina, prodNome.size());

                    for (int i = inicio; i < fim; i++) {
                        if (prodNome.get(i) != null) {
                            System.out.printf("[%d] %s | Preço: %s | Estoque: %d un\n", (i + 1), prodNome.get(i), formatarMoeda(prodPrecos.get(i)), prodEstoque.get(i));
                        }
                    }
                    System.out.println("----------------------------------------------------------------");
                    System.out.println("[N] Próxima | [P] Anterior | [NÚMERO] Abrir Modal | [0] Voltar");
                    System.out.print("Comando: ");
                    String cmd = scanner.nextLine().trim().toUpperCase();

                    if (cmd.equals("N") && pagina < totalPaginas) pagina++;
                    else if (cmd.equals("P") && pagina > 1) pagina--;
                    else if (cmd.equals("0") || cmd.equals("SAIR")) navegandoProd = false;
                    else if (cmd.matches("^[0-9]+$")) {
                        int idx = Integer.parseInt(cmd) - 1;
                        if (idx >= 0 && idx < prodNome.size() && prodNome.get(idx) != null) {
                            boolean emModalProd = true;
                            while (emModalProd) {
                                System.out.println("\n┌────────────────────────────────────────────────────────┐");
                                System.out.println("│               [MODAL DE GESTÃO DE PRODUTO]             │");
                                System.out.println("├────────────────────────────────────────────────────────┤");
                                System.out.printf("│ Produto: %s\n", prodNome.get(idx));
                                System.out.printf("│ Preço: %s | Estoque: %d\n", formatarMoeda(prodPrecos.get(idx)), prodEstoque.get(idx));
                                System.out.println("├────────────────────────────────────────────────────────┤");
                                System.out.println("│ [1] Ver Detalhes Completos                             │");
                                System.out.println("│ [2] Editar Preço de Venda                              │");
                                System.out.println("│ [3] Ajustar Quantidade em Estoque                      │");
                                System.out.println("│ [0] Fechar Modal                                       │");
                                System.out.println("└────────────────────────────────────────────────────────┘");
                                int acaoModal = lerInteiroValido(scanner, "Ação: ");
                                if (acaoModal == 1) {
                                    System.out.println("\n--- FICHA DO PRODUTO ---");
                                    System.out.println("• Descrição : " + prodNome.get(idx));
                                    System.out.println("• Classificação NCM : " + prodNcm.get(idx));
                                    System.out.println("• Valor Unitário    : " + formatarMoeda(prodPrecos.get(idx)));
                                    System.out.println("• Saldo em Estoque  : " + prodEstoque.get(idx) + " unidades\n");
                                } else if (acaoModal == 2) {
                                    if (!verificarAutoridadeGerencial(cargoLogado)) continue;
                                    double novoPreco = lerDoubleValido(scanner, "Novo Preço (R$): ");
                                    prodPrecos.set(idx, novoPreco);
                                    System.out.println("✅ Preço atualizado com sucesso!\n");
                                } else if (acaoModal == 3) {
                                    if (!verificarAutoridadeGerencial(cargoLogado)) continue;
                                    int novoEstoque = lerInteiroValido(scanner, "Novo Estoque (unidades): ");
                                    prodEstoque.set(idx, novoEstoque);
                                    System.out.println("✅ Estoque atualizado com sucesso!\n");
                                } else if (acaoModal == 0 || acaoModal == -999) {
                                    emModalProd = false;
                                }
                            }
                        }
                    }
                }
            } else if (op == 2) {
                if (!verificarAutoridadeGerencial(cargoLogado)) continue;
                gerarRollbackAutomatico(funcId, funcNome, funcCpf, funcCargo, funcSenha, funcSalario, funcPago, funcStatus, funcMotivoInativo, prodNome, prodNcm, prodPrecos, prodEstoque, caixaGlobalRef);
                int idPr = lerInteiroValido(scanner, "Posição do produto: ") - 1;
                if (idPr >= 0) {
                    String nProd = lerTextoApenasLetras(scanner, "Nome: ");
                    double pVenda = lerDoubleValido(scanner, "Preço: ");
                    int eIni = lerInteiroValido(scanner, "Estoque inicial: ");
                    while (prodNome.size() <= idPr) { prodNome.add(null); prodNcm.add(""); prodPrecos.add(0.0); prodEstoque.add(0); }
                    prodNome.set(idPr, nProd); prodNcm.set(idPr, "2106.90.30"); prodPrecos.set(idPr, pVenda); prodEstoque.set(idPr, eIni);
                    registrarLogAuditoria(logHistorico, usuarioLogado, "Novo produto: " + nProd);
                    System.out.println("✅ Produto cadastrado!\n");
                }
            } else if (op == 3) {
                int pagina = 1;
                int porPagina = 25;
                boolean lendoTransacoes = true;
                while (lendoTransacoes) {
                    int totalPaginas = Math.max(1, (int) Math.ceil((double) transacaoTipo.size() / porPagina));
                    if (pagina > totalPaginas) pagina = totalPaginas;

                    System.out.println("\n--- RELATÓRIO DE TRANSAÇÕES CONSOLIDADAS (Página " + pagina + "/" + totalPaginas + ") ---");
                    int inicio = (pagina - 1) * porPagina;
                    int fim = Math.min(inicio + porPagina, transacaoTipo.size());

                    for (int i = inicio; i < fim; i++) {
                        System.out.printf("[%d] TIPO: %s | Valor: %s\n", (i + 1), transacaoTipo.get(i), formatarMoeda(transacaoValor.get(i)));
                        System.out.printf("    • Envolvidos: %s\n", transacaoOrigemDestino.get(i));
                        System.out.printf("    • Detalhes  : %s\n", transacaoDetalhes.get(i));
                        System.out.println("----------------------------------------------------------------");
                    }
                    System.out.println("[N] Próxima | [P] Anterior | [0] Voltar");
                    String cmd = scanner.nextLine().trim().toUpperCase();
                    if (cmd.equals("N") && pagina < totalPaginas) pagina++;
                    else if (cmd.equals("P") && pagina > 1) pagina--;
                    else if (cmd.equals("0") || cmd.equals("SAIR")) lendoTransacoes = false;
                }
            } else if (op == 4) {
                realizarBackupTerminal(funcId, funcNome, funcCpf, funcCargo, funcSenha, funcSalario, funcPago, funcStatus, funcMotivoInativo, prodNome, prodNcm, prodPrecos, prodEstoque, caixaGlobalRef, transacaoTipo, logHistorico, scanner, usuarioLogado, senhaLogado);
            } else if (op == 5) {
                restaurarBackupTerminal(funcId, funcNome, funcCpf, funcCargo, funcSenha, funcSalario, funcPago, funcStatus, funcMotivoInativo, prodNome, prodNcm, prodPrecos, prodEstoque, caixaGlobalRef, logHistorico, scanner, cargoLogado, usuarioLogado, senhaLogado);
            }
        }
    }

    public static void fluxoModuloCliente(Scanner scanner, List<String> prodNome, List<Double> prodPrecos, List<Integer> prodEstoque, 
                                          List<Double> caixaGlobalRef, List<Double> receitaVendasRef, List<String> clienteCpf, List<String> clienteNome,
                                          List<String> transacaoTipo, List<String> transacaoOrigemDestino, 
                                          List<String> transacaoDetalhes, List<Double> transacaoValor) {
        boolean comprando = true;
        while (comprando) {
            System.out.println("\n--- CATÁLOGO DE CLIENTE ---");
            for (int i = 0; i < Math.min(15, prodNome.size()); i++) {
                if (prodNome.get(i) != null) System.out.printf("[%d] %s | Preço: %s | Estoque: %d\n", (i + 1), prodNome.get(i), formatarMoeda(prodPrecos.get(i)), prodEstoque.get(i));
            }
            System.out.println("[0] Voltar");
            int esc = lerInteiroValido(scanner, "Produto [0 para sair]: ");
            if (esc == 0 || esc == -999) return;
            int idx = esc - 1;
            if (idx >= 0 && idx < prodNome.size() && prodNome.get(idx) != null) {
                if (prodEstoque.get(idx) <= 0) {
                    System.out.println("❌ Produto esgotado!\n");
                    continue;
                }
                int qtd = lerInteiroValido(scanner, "Quantidade desejada: ");
                if (qtd > prodEstoque.get(idx)) {
                    System.out.println("❌ Quantidade superior ao estoque.\n");
                    continue;
                }

                String cpf = lerCpfValido(scanner);
                if (cpf.equals("CANCELAR")) continue;

                String nomeCliente = "CLIENTE AVULSO";
                int idxCliente = clienteCpf.indexOf(cpf);
                if (idxCliente != -1) nomeCliente = clienteNome.get(idxCliente);

                double total = prodPrecos.get(idx) * qtd;
                prodEstoque.set(idx, prodEstoque.get(idx) - qtd);
                caixaGlobalRef.set(0, caixaGlobalRef.get(0) + total);
                receitaVendasRef.set(0, receitaVendasRef.get(0) + total);

                transacaoTipo.add("VENDA");
                transacaoOrigemDestino.add("Cliente: " + nomeCliente + " (CPF: " + cpf + ")");
                transacaoDetalhes.add(qtd + "x " + prodNome.get(idx));
                transacaoValor.add(total);

                System.out.printf("✅ Compra realizada com sucesso! Total: %s\n\n", formatarMoeda(total));
            }
        }
    }

    /* =========================================================================
       [MÓDULO 8] MÉTODO PRINCIPAL
       ========================================================================= */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        exibirCabecalho();

        List<String> funcId = new ArrayList<>();
        List<String> funcNome = new ArrayList<>();
        List<String> funcCpf = new ArrayList<>();
        List<String> funcCargo = new ArrayList<>();
        List<String> funcSenha = new ArrayList<>();
        List<Double> funcSalario = new ArrayList<>();
        List<Boolean> funcPago = new ArrayList<>();
        List<String> funcStatus = new ArrayList<>();
        List<String> funcMotivoInativo = new ArrayList<>();

        List<String> fornNome = new ArrayList<>();
        List<String> fornCnpj = new ArrayList<>();
        List<String> fornUf = new ArrayList<>();
        List<String> fornProdutoFornecido = new ArrayList<>();
        List<Double> fornPrecoPago = new ArrayList<>();
        List<Double> fornLucroPretendidoPct = new ArrayList<>();
        List<Boolean> fornPago = new ArrayList<>();

        List<String> clienteCpf = new ArrayList<>();
        List<String> clienteNome = new ArrayList<>();

        List<String> prodNome = new ArrayList<>();
        List<String> prodNcm = new ArrayList<>();
        List<Double> prodPrecos = new ArrayList<>();
        List<Integer> prodEstoque = new ArrayList<>();

        List<Double> caixaGlobal = new ArrayList<>(Arrays.asList(200000.00));
        List<Double> gastosEstoque = new ArrayList<>(Arrays.asList(35000.00));
        List<Double> receitaVendas = new ArrayList<>(Arrays.asList(0.0));
        List<Double> cmv = new ArrayList<>(Arrays.asList(0.0));
        List<Double> salariosPagos = new ArrayList<>(Arrays.asList(0.0));
        List<Double> fornecedoresPagos = new ArrayList<>(Arrays.asList(0.0));
        List<Double> impostosPendentes = new ArrayList<>(Arrays.asList(2500.00));
        int[] anoFiscal = {2026};

        List<String> transacaoTipo = new ArrayList<>();
        List<String> transacaoOrigemDestino = new ArrayList<>();
        List<String> transacaoDetalhes = new ArrayList<>();
        List<Double> transacaoValor = new ArrayList<>();
        List<String> logHistorico = new ArrayList<>();

        carregarMassaFuncionarios(funcId, funcNome, funcCpf, funcCargo, funcSenha, funcSalario, funcPago, funcStatus, funcMotivoInativo);
        carregarMassaFornecedores(fornNome, fornCnpj, fornUf, fornProdutoFornecido, fornPrecoPago, fornLucroPretendidoPct, fornPago);
        carregarMassaClientes(clienteCpf, clienteNome);

        System.out.println("\n================================================================");
        System.out.println("              MODO DE INICIALIZAÇÃO DO SISTEMA                  ");
        System.out.println("================================================================");
        System.out.println("[1] Carregar Sistema dentro do Padrão Sincronizado");
        System.out.println("[2] Gerar Sistema com Massa Totalmente RANDÔMICA");
        System.out.println("================================================================");
        
        int escolhaModo = lerInteiroValido(scanner, "Selecione o modo [1 ou 2]: ");
        if (escolhaModo == 2) {
            carregarMassaDadosRandom(prodNome, prodNcm, prodPrecos, prodEstoque, clienteCpf, clienteNome,
                                     transacaoTipo, transacaoOrigemDestino, transacaoDetalhes, transacaoValor, 
                                     caixaGlobal, receitaVendas, logHistorico);
        } else {
            carregarMassaDadosPadrao(prodNome, prodNcm, prodPrecos, prodEstoque, clienteCpf, clienteNome,
                                     transacaoTipo, transacaoOrigemDestino, transacaoDetalhes, transacaoValor, 
                                     caixaGlobal, receitaVendas, logHistorico);
        }

        boolean sistemaAtivo = true;
        while (sistemaAtivo) {
            System.out.println("\n================================================================");
            System.out.println("                 MENU PRINCIPAL DE ACESSO                       ");
            System.out.println("================================================================");
            System.out.println("[1] Acessar como Cliente");
            System.out.println("[2] Acessar Painel Administrativo (Login)");
            System.out.println("[3] Sair do Sistema");
            System.out.println("================================================================");

            int opcaoInicial = lerInteiroValido(scanner, "Opção: ");

            if (opcaoInicial == 1) {
                fluxoModuloCliente(scanner, prodNome, prodPrecos, prodEstoque, caixaGlobal, receitaVendas, clienteCpf, clienteNome, transacaoTipo, transacaoOrigemDestino, transacaoDetalhes, transacaoValor);
            } else if (opcaoInicial == 2) {
                System.out.println("\n=== AUTENTICAÇÃO ADMINISTRATIVA ===");
                for (int i = 0; i < funcNome.size(); i++) {
                    if (funcStatus.get(i).equalsIgnoreCase("ATIVO")) System.out.printf("- ID: %s | Nome: %s | Cargo: %s\n", funcId.get(i), funcNome.get(i), funcCargo.get(i));
                }
                System.out.println("----------------------------------------------------------------");

                String idInput = "";
                while (true) {
                    System.out.print("ID Funcional (Ex: 001) [0 para sair]: ");
                    idInput = scanner.nextLine().trim().toUpperCase();
                    if (idInput.equals("0") || idInput.equalsIgnoreCase("SAIR")) break;
                    if (idInput.length() == 3 && funcId.contains(idInput)) break;
                    System.out.println("❌ ID inválido.\n");
                }
                if (idInput.equals("0") || idInput.equalsIgnoreCase("SAIR")) continue;

                int index = funcId.indexOf(idInput);
                String nomeLogado = funcNome.get(index);
                String cargoLogado = funcCargo.get(index);
                boolean autenticado = false;
                String senhaLogado = "";

                for (int t = 1; t <= 3; t++) {
                    System.out.print("Senha (Tentativa " + t + " de 3): ");
                    String sInput = scanner.nextLine().trim();
                    if (funcSenha.get(index).equals(sInput)) {
                        autenticado = true; senhaLogado = sInput;
                        registrarLogAuditoria(logHistorico, nomeLogado, "Login efetuado");
                        System.out.println("\n✅ Login bem-sucedido! Bem-vindo(a), " + nomeLogado + " (" + cargoLogado + ").\n");
                        break;
                    }
                    System.out.println("❌ Senha incorreta.\n");
                }

                if (!autenticado) continue;

                boolean adminAtivo = true;
                while (adminAtivo) {
                    System.out.println("\n================================================================");
                    System.out.println("                 PAINEL ADMINISTRATIVO (" + cargoLogado + ")      ");
                    System.out.println("================================================================");
                    if (cargoLogado.equalsIgnoreCase("GERENTE")) {
                        System.out.println("[1] Gerenciar Autoridades e Funcionários");
                        System.out.println("[2] Gerenciar Fornecedores e Abastecimentos");
                        System.out.println("[3] Painel Financeiro, Estoque e Relatórios");
                    } else if (cargoLogado.equalsIgnoreCase("FINANCEIRO")) {
                        System.out.println("[2] Gerenciar Fornecedores e Abastecimentos");
                        System.out.println("[3] Painel Financeiro, Estoque e Relatórios");
                    } else {
                        System.out.println("(Operador sem permissões administrativas)");
                    }
                    System.out.println("[4] Sair do Painel");
                    System.out.println("================================================================");

                    int opAdmin = lerInteiroValido(scanner, "Opção: ");
                    if (opAdmin == 1 && cargoLogado.equalsIgnoreCase("GERENTE")) {
                        menuGerenciarFuncionarios(scanner, funcId, funcNome, funcCpf, funcCargo, funcSenha, funcSalario, funcPago, funcStatus, funcMotivoInativo, logHistorico, nomeLogado, cargoLogado, prodNome, prodNcm, prodPrecos, prodEstoque, caixaGlobal, transacaoTipo, transacaoOrigemDestino, transacaoDetalhes, transacaoValor);
                    } else if (opAdmin == 2 && !cargoLogado.equalsIgnoreCase("OPERADOR")) {
                        menuGerenciarFornecedores(scanner, fornNome, fornCnpj, fornUf, fornProdutoFornecido, fornPrecoPago, fornLucroPretendidoPct, fornPago, logHistorico, nomeLogado, cargoLogado, prodNome, prodEstoque, caixaGlobal, transacaoTipo, transacaoOrigemDestino, transacaoDetalhes, transacaoValor);
                    } else if (opAdmin == 3 && !cargoLogado.equalsIgnoreCase("OPERADOR")) {
                        menuFinanceiroEOperacional(scanner, prodNome, prodNcm, prodPrecos, prodEstoque, caixaGlobal, gastosEstoque, receitaVendas, cmv, salariosPagos, fornecedoresPagos, impostosPendentes, funcPago, fornPago, funcSalario, fornPrecoPago, anoFiscal, transacaoTipo, transacaoOrigemDestino, transacaoDetalhes, transacaoValor, clienteCpf, clienteNome, fornNome, fornCnpj, fornUf, fornProdutoFornecido, fornLucroPretendidoPct, logHistorico, nomeLogado, cargoLogado, senhaLogado, funcId, funcNome, funcCpf, funcCargo, funcSenha, funcStatus, funcMotivoInativo);
                    } else if (opAdmin == 4 || opAdmin == -999) {
                        adminAtivo = false;
                    }
                }
            } else if (opcaoInicial == 3 || opcaoInicial == -999) {
                sistemaAtivo = false;
                System.out.println("\n[INFO] Sistema encerrado com segurança. Obrigado!\n");
            }
        }
        scanner.close();
    }
}