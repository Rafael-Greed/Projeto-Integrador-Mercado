import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class prototo {

    /* =========================================================================
       [MÓDULO 1] TRIBUTÁRIO E DE MATRIZ FISCAL (RECEITA FEDERAL)
       ========================================================================= */
    public static double aliquotaIcmsInterno2026 = 0.18;
    public static double aliquotaIcmsInterno2027 = 0.19; 
    public static double aliquotaPisPadrao = 0.0165;
    public static double aliquotaCofinsPadrao = 0.0760;

    public static double buscarAliquotaIcms(String ncm, String ufOrigem, String ufDestino, int anoExercicio) {
        double aliquotaBase = (anoExercicio == 2027) ? aliquotaIcmsInterno2027 : aliquotaIcmsInterno2026;
        if (ufOrigem.equalsIgnoreCase("SP") && ufDestino.equalsIgnoreCase("SP")) {
            return aliquotaBase; 
        } else {
            return 0.12; 
        }
    }

    public static double calcularImpostoItem(double valorBase, double aliquota) {
        return valorBase * aliquota;
    }

    /* =========================================================================
       [MÓDULO 2] SEGURANÇA E UTILITÁRIOS (STRING API)
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

    /* =========================================================================
       [MÓDULO 3] VALIDAÇÃO RÍGIDA COM SAÍDA DE EMERGÊNCIA GLOBAL
       ========================================================================= */
    public static String lerTextoApenasLetras(Scanner scanner, String mensagem) {
        String entrada = "";
        while (true) {
            try {
                System.out.print(mensagem);
                entrada = scanner.nextLine().trim();

                if (entrada.equalsIgnoreCase("0") || entrada.equalsIgnoreCase("SAIR")) {
                    return "CANCELAR";
                }

                if (entrada.isEmpty()) {
                    System.out.println("⚠️  [Aviso]: O campo não pode ficar vazio.");
                    System.out.println("💡 [Dica]: Digite '0' ou 'SAIR' para cancelar e voltar.\n");
                } else if (!entrada.matches("^[a-zA-ZáàâãéèêíïóôõúüçÁÀÂÃÉÈÊÍÏÓÔÕÚÜÇ\\s]+$")) {
                    System.out.println("❌ [Erro]: Este campo aceita apenas letras e espaços.");
                    System.out.println("💡 [Dica]: Digite '0' ou 'SAIR' para cancelar e voltar.\n");
                } else {
                    break;
                }
            } catch (Exception e) {
                System.out.println("❌ [Erro]: Falha na leitura do texto. Tente novamente.\n");
            }
        }
        return entrada.toUpperCase();
    }

    public static String lerUfValida(Scanner scanner, String mensagem) {
        String uf = "";
        List<String> ufsValidas = Arrays.asList(
            "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", 
            "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN", 
            "RS", "RO", "RR", "SC", "SP", "SE", "TO"
        );
        while (true) {
            try {
                System.out.print(mensagem);
                uf = scanner.nextLine().trim().toUpperCase();

                if (uf.equalsIgnoreCase("0") || uf.equalsIgnoreCase("SAIR")) {
                    return "CANCELAR";
                }

                if (ufsValidas.contains(uf)) {
                    break;
                } else {
                    System.out.println("❌ [Erro]: UF inválida! Digite uma sigla de estado válida.");
                    System.out.println("💡 [Dica de Emergência]: Digite '0' para voltar ao menu anterior.\n");
                }
            } catch (Exception e) {
                System.out.println("❌ [Erro]: Falha na validação da UF.\n");
            }
        }
        return uf;
    }

    public static int lerInteiroValido(Scanner scanner, String mensagem) {
        int valor = 0;
        while (true) {
            try {
                System.out.print(mensagem);
                String entrada = scanner.nextLine().trim();
                
                if (entrada.equalsIgnoreCase("0") || entrada.equalsIgnoreCase("SAIR")) {
                    return -999; // Sinalizador universal de retorno/cancelamento
                }

                String apenasNumeros = entrada.replaceAll("[^0-9]", "");
                
                if (apenasNumeros.isEmpty()) {
                    System.out.println("❌ [Erro]: Digite apenas números inteiros válidos.");
                    System.out.println("💡 [Dica]: Digite '0' para voltar.\n");
                    continue;
                }
                valor = Integer.parseInt(apenasNumeros);
                break;
            } catch (Exception e) {
                System.out.println("❌ [Erro]: Entrada inválida. Digite apenas números.");
                System.out.println("💡 [Dica]: Digite '0' para voltar.\n");
            }
        }
        return valor;
    }

    public static double lerDoubleValido(Scanner scanner, String mensagem) {
        double valor = 0.0;
        while (true) {
            try {
                System.out.print(mensagem);
                String entrada = scanner.nextLine().trim().replace(",", ".");
                
                if (entrada.equalsIgnoreCase("0") || entrada.equalsIgnoreCase("SAIR")) {
                    return -999.0;
                }

                valor = Double.parseDouble(entrada);
                if (valor < 0) {
                    System.out.println("❌ [Erro]: O valor não pode ser negativo.");
                    System.out.println("💡 [Dica]: Digite '0' para voltar.\n");
                    continue;
                }
                break;
            } catch (Exception e) {
                System.out.println("❌ [Erro]: Formato numérico inválido (exemplo correto: 49.90).");
                System.out.println("💡 [Dica]: Digite '0' para voltar.\n");
            }
        }
        return valor;
    }

    public static String lerCpfValido(Scanner scanner) {
        String cpf = "";
        while (true) {
            try {
                System.out.print("Digite o CPF (11 números) [0 ou SAIR para voltar]: ");
                String entrada = scanner.nextLine().trim();

                if (entrada.equalsIgnoreCase("0") || entrada.equalsIgnoreCase("SAIR")) {
                    return "CANCELAR";
                }

                cpf = entrada.replaceAll("[^0-9]", "");

                if (cpf.length() != 11 || cpf.matches("^(.)\\1{10}$")) {
                    System.out.println("❌ [Erro]: CPF inválido. Deve conter exatamente 11 dígitos.");
                    System.out.println("🚨 [Aviso de Saída]: Digite '0' ou 'SAIR' a qualquer momento para abortar e voltar.\n");
                    continue;
                }

                int soma1 = 0, soma2 = 0;
                for (int i = 0; i < 9; i++) {
                    int num = Character.getNumericValue(cpf.charAt(i));
                    soma1 += num * (10 - i);
                    soma2 += num * (11 - i);
                }
                int dv1 = 11 - (soma1 % 11);
                if (dv1 > 9) dv1 = 0;

                soma2 += dv1 * 2;
                int dv2 = 11 - (soma2 % 11);
                if (dv2 > 9) dv2 = 0;

                if (dv1 == Character.getNumericValue(cpf.charAt(9)) && dv2 == Character.getNumericValue(cpf.charAt(10))) {
                    break;
                } else {
                    System.out.println("❌ [Erro]: Dígitos verificadores do CPF incorretos.");
                    System.out.println("🚨 [Aviso de Saída]: Digite '0' ou 'SAIR' a qualquer momento para abortar e voltar.\n");
                }
            } catch (Exception e) {
                System.out.println("❌ [Erro]: Falha ao validar o CPF.\n");
            }
        }
        return cpf;
    }

    public static String lerCnpjValido(Scanner scanner) {
        String cnpj = "";
        while (true) {
            try {
                System.out.print("Digite o CNPJ (14 números) [0 ou SAIR para voltar]: ");
                String entrada = scanner.nextLine().trim();

                if (entrada.equalsIgnoreCase("0") || entrada.equalsIgnoreCase("SAIR")) {
                    return "CANCELAR";
                }

                cnpj = entrada.replaceAll("[^0-9]", "");

                if (cnpj.length() != 14 || cnpj.matches("^(.)\\1{13}$")) {
                    System.out.println("❌ [Erro]: CNPJ inválido. Deve conter 14 dígitos.");
                    System.out.println("🚨 [Aviso de Saída]: Digite '0' ou 'SAIR' a qualquer momento para abortar e voltar.\n");
                    continue;
                }

                int[] peso1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
                int[] peso2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
                
                int soma = 0;
                for (int i = 0; i < 12; i++) {
                    soma += Character.getNumericValue(cnpj.charAt(i)) * peso1[i];
                }
                int dv1 = soma % 11;
                dv1 = (dv1 < 2) ? 0 : (11 - dv1);

                soma = 0;
                for (int i = 0; i < 13; i++) {
                    soma += Character.getNumericValue(cnpj.charAt(i)) * peso2[i];
                }
                int dv2 = soma % 11;
                dv2 = (dv2 < 2) ? 0 : (11 - dv2);

                if (dv1 == Character.getNumericValue(cnpj.charAt(12)) && dv2 == Character.getNumericValue(cnpj.charAt(13))) {
                    break;
                } else {
                    System.out.println("❌ [Erro]: Dígitos verificadores do CNPJ incorretos.");
                    System.out.println("🚨 [Aviso de Saída]: Digite '0' ou 'SAIR' a qualquer momento para abortar e voltar.\n");
                }
            } catch (Exception e) {
                System.out.println("❌ [Erro]: Falha ao validar o CNPJ.\n");
            }
        }
        return cnpj;
    }

    public static String lerCepValido(Scanner scanner) {
        String cep = "";
        while (true) {
            try {
                System.out.print("Digite o CEP (8 números) [0 ou SAIR para voltar]: ");
                String entrada = scanner.nextLine().trim();

                if (entrada.equalsIgnoreCase("0") || entrada.equalsIgnoreCase("SAIR")) {
                    return "CANCELAR";
                }

                cep = entrada.replaceAll("[^0-9]", "");

                if (cep.length() != 8) {
                    System.out.println("❌ [Erro]: CEP inválido. Deve conter 8 dígitos.");
                    System.out.println("🚨 [Aviso de Saída]: Digite '0' ou 'SAIR' a qualquer momento para abortar e voltar.\n");
                } else {
                    break;
                }
            } catch (Exception e) {
                System.out.println("❌ [Erro]: Falha na leitura do CEP.\n");
            }
        }
        return cep;
    }

    public static double obterSalarioPadraoCargo(String cargo) {
        if (cargo.equalsIgnoreCase("GERENTE")) return 5500.00;
        if (cargo.equalsIgnoreCase("FINANCEIRO")) return 4800.00;
        return 2200.00; 
    }

    public static void exibirCabecalho() {
        System.out.println("\n================================================================");
        System.out.println("            Bem vindo ao Protei Creat                           ");
        System.out.println("          SISTEMA DE GESTÃO TRIBUTÁVEL E ESTOQUE - L'ESTOQUE    ");
        System.out.println("================================================================");
    }

    /* =========================================================================
       [MÓDULO] GESTÃO DE FUNCIONÁRIOS
       ========================================================================= */
    public static void menuGerenciarFuncionarios(Scanner scanner, List<String> funcId, List<String> funcNome, List<String> funcCpf, 
                                                 List<String> funcCargo, List<String> funcSenha, List<Double> funcSalario, 
                                                 List<Boolean> funcPago) {
        boolean rodando = true;
        while (rodando) {
            System.out.println("\n================================================================");
            System.out.println("                   PAINEL: GESTÃO DE FUNCIONÁRIOS               ");
            System.out.println("================================================================");
            System.out.println("[1] Listar funcionários cadastrados");
            System.out.println("[2] Cadastrar novo funcionário");
            System.out.println("[3] Alterar salário de um funcionário");
            System.out.println("[4] Excluir funcionário");
            System.out.println("[5] Voltar ao menu administrativo anterior");
            System.out.println("================================================================");

            int op = lerInteiroValido(scanner, "Selecione uma opção (0 para voltar): ");

            if (op == 5 || op == -999) {
                rodando = false;
            } else if (op == 1) {
                System.out.println("\n================================================================");
                System.out.println("                  LISTA DE FUNCIONÁRIOS ATIVOS                  ");
                System.out.println("================================================================");
                for (int i = 0; i < funcNome.size(); i++) {
                    String statusPg = funcPago.get(i) ? "Pago" : "Pendente";
                    System.out.printf("[%d] ID: %s | Nome: %s | Cargo: %s | Salário: %s (Status: %s)\n",
                            (i + 1), funcId.get(i), funcNome.get(i), funcCargo.get(i), formatarMoeda(funcSalario.get(i)), statusPg);
                }
                System.out.println("================================================================");
            } else if (op == 2) {
                try {
                    System.out.print("ID Funcional Exclusivo [Ex: F04] (0 para voltar): "); 
                    String novoId = scanner.nextLine().trim().toUpperCase();

                    if (novoId.equals("0") || novoId.equalsIgnoreCase("SAIR")) continue;

                    if (funcId.contains(novoId)) {
                        System.out.println("❌ [Erro]: Este ID já está em uso.\n");
                        continue;
                    }

                    String novoNome = lerTextoApenasLetras(scanner, "Nome completo [Ex: Carlos Souza] (0 para voltar): ");
                    if (novoNome.equals("CANCELAR")) continue;

                    String novoCpf = lerCpfValido(scanner);
                    if (novoCpf.equals("CANCELAR")) continue;

                    if (funcCpf.contains(novoCpf)) {
                        System.out.println("❌ [Erro]: Este CPF já está cadastrado.\n");
                        continue;
                    }

                    int cOpt = lerInteiroValido(scanner, "Cargo [1 - Gerente | 2 - Operador | 3 - Financeiro] (0 para voltar): ");
                    if (cOpt == -999) continue;
                    
                    String cargoDefinido = "OPERADOR";
                    if (cOpt == 1) cargoDefinido = "GERENTE";
                    else if (cOpt == 3) cargoDefinido = "FINANCEIRO";
                    
                    double salarioPadrao = obterSalarioPadraoCargo(cargoDefinido);
                    String senhaGerada = geradorSenhaAleatoriaSistema();

                    funcId.add(novoId);
                    funcNome.add(novoNome);
                    funcCpf.add(novoCpf);
                    funcCargo.add(cargoDefinido);
                    funcSenha.add(senhaGerada);
                    funcSalario.add(salarioPadrao);
                    funcPago.add(false);

                    System.out.println("\n✅ Sucesso! Funcionário cadastrado.");
                    System.out.println("🔑 Senha gerada: " + senhaGerada + " | Salário: " + formatarMoeda(salarioPadrao) + "\n");
                } catch (Exception e) {
                    System.out.println("❌ [Erro]: Falha ao cadastrar.\n");
                }
            } else if (op == 3) {
                System.out.println("\n================================================================");
                System.out.println("               ATUALIZAÇÃO DE SALÁRIO DE FUNCIONÁRIO            ");
                System.out.println("================================================================");
                for (int i = 0; i < funcNome.size(); i++) {
                    System.out.printf("[%d] %s (%s) - Salário: %s\n", (i + 1), funcNome.get(i), funcCargo.get(i), formatarMoeda(funcSalario.get(i)));
                }
                System.out.println("================================================================");
                int idx = lerInteiroValido(scanner, "Informe o número (índice) do funcionário (0 para voltar): ") - 1;
                if (idx == -1000) continue; 
                if (idx >= 0 && idx < funcNome.size()) {
                    double nSal = lerDoubleValido(scanner, "Informe o novo valor do salário (R$): ");
                    if (nSal == -999.0) continue;
                    funcSalario.set(idx, nSal);
                    System.out.println("✅ Sucesso! Salário atualizado.\n");
                } else {
                    System.out.println("❌ [Erro]: Funcionário não encontrado.\n");
                }
            } else if (op == 4) {
                System.out.println("\n--- LISTA DE FUNCIONÁRIOS PARA EXCLUSÃO ---");
                for (int i = 0; i < funcNome.size(); i++) {
                    System.out.printf("[%d] ID: %s | %s\n", (i + 1), funcId.get(i), funcNome.get(i));
                }
                int idx = lerInteiroValido(scanner, "\nInforme o número (índice) do funcionário que deseja excluir (0 para voltar): ") - 1;
                if (idx == -1000) continue;
                if (idx >= 0 && idx < funcNome.size()) {
                    funcId.remove(idx);
                    funcNome.remove(idx);
                    funcCpf.remove(idx);
                    funcCargo.remove(idx);
                    funcSenha.remove(idx);
                    funcSalario.remove(idx);
                    funcPago.remove(idx);
                    System.out.println("✅ Sucesso! Funcionário removido.\n");
                } else {
                    System.out.println("❌ [Erro]: Funcionário não encontrado.\n");
                }
            }
        }
    }

    /* =========================================================================
       [MÓDULO] GESTÃO DE FORNECEDORES
       ========================================================================= */
    public static void menuGerenciarFornecedores(Scanner scanner, List<String> fornNome, List<String> fornCnpj, List<String> fornUf, 
                                                 List<String> fornProdutoFornecido, List<Double> fornPrecoPago, 
                                                 List<Double> fornLucroPretendidoPct) {
        boolean rodando = true;
        while (rodando) {
            System.out.println("\n================================================================");
            System.out.println("                   PAINEL: GESTÃO DE FORNECEDORES               ");
            System.out.println("================================================================");
            System.out.println("[1] Listar fornecedores cadastrados");
            System.out.println("[2] Cadastrar novo fornecedor");
            System.out.println("[3] Alterar preço de custo do fornecedor");
            System.out.println("[4] Excluir fornecedor");
            System.out.println("[5] Voltar ao menu administrativo anterior");
            System.out.println("================================================================");

            int op = lerInteiroValido(scanner, "Selecione uma opção (0 para voltar): ");

            if (op == 5 || op == -999) {
                rodando = false;
            } else if (op == 1) {
                System.out.println("\n================================================================");
                System.out.println("                 LISTA DE FORNECEDORES ATIVOS                   ");
                System.out.println("================================================================");
                for (int i = 0; i < fornNome.size(); i++) {
                    System.out.printf("[%d] Razão Social: %s | CNPJ: %s | UF: %s | Produto: %s\n",
                            (i + 1), fornNome.get(i), fornCnpj.get(i), fornUf.get(i), fornProdutoFornecido.get(i));
                }
                System.out.println("================================================================");
            } else if (op == 2) {
                try {
                    String nomeForn = lerTextoApenasLetras(scanner, "Razão Social / Nome [Ex: Suplemax Ltda] (0 para voltar): ");
                    if (nomeForn.equals("CANCELAR")) continue;

                    String cnpjForn = lerCnpjValido(scanner);
                    if (cnpjForn.equals("CANCELAR")) continue;

                    if (fornCnpj.contains(cnpjForn)) {
                        System.out.println("❌ [Erro]: Este CNPJ já está cadastrado.\n");
                        continue;
                    }

                    String uf = lerUfValida(scanner, "UF de origem [Ex: SP]: ");
                    if (uf.equals("CANCELAR")) continue;

                    System.out.print("Produto fornecido [Ex: Whey Protein] (0 para voltar): "); 
                    String prod = scanner.nextLine().trim().toUpperCase();
                    if (prod.equals("0") || prod.equalsIgnoreCase("SAIR")) continue;
                    
                    double custo = lerDoubleValido(scanner, "Preço pago ao fornecedor (Custo R$): ");
                    if (custo == -999.0) continue;

                    double lucro = lerDoubleValido(scanner, "Lucro pretendido para o mercado (%): ");
                    if (lucro == -999.0) continue;

                    fornNome.add(nomeForn);
                    fornCnpj.add(cnpjForn);
                    fornUf.add(uf);
                    fornProdutoFornecido.add(prod);
                    fornPrecoPago.add(custo);
                    fornLucroPretendidoPct.add(lucro);

                    System.out.println("✅ Sucesso! Fornecedor cadastrado.\n");
                } catch (Exception e) {
                    System.out.println("❌ [Erro]: Falha ao registrar fornecedor.\n");
                }
            } else if (op == 3) {
                System.out.println("\n================================================================");
                System.out.println("               ATUALIZAÇÃO DE CUSTO DE FORNECEDOR               ");
                System.out.println("================================================================");
                for (int i = 0; i < fornNome.size(); i++) {
                    System.out.printf("[%d] %s (Custo: %s)\n", (i + 1), fornNome.get(i), formatarMoeda(fornPrecoPago.get(i)));
                }
                System.out.println("================================================================");
                int idx = lerInteiroValido(scanner, "Informe o número (índice) do fornecedor (0 para voltar): ") - 1;
                if (idx == -1000) continue;
                if (idx >= 0 && idx < fornNome.size()) {
                    double nCusto = lerDoubleValido(scanner, "Informe o novo preço de custo (R$): ");
                    if (nCusto == -999.0) continue;
                    fornPrecoPago.set(idx, nCusto);
                    System.out.println("✅ Sucesso! Preço de custo atualizado.\n");
                } else {
                    System.out.println("❌ [Erro]: Fornecedor não encontrado.\n");
                }
            } else if (op == 4) {
                System.out.println("\n--- LISTA DE FORNECEDORES PARA EXCLUSÃO ---");
                for (int i = 0; i < fornNome.size(); i++) {
                    System.out.printf("[%d] %s | CNPJ: %s\n", (i + 1), fornNome.get(i), fornCnpj.get(i));
                }
                int idx = lerInteiroValido(scanner, "\nInforme o número (índice) do fornecedor que deseja excluir (0 para voltar): ") - 1;
                if (idx == -1000) continue;
                if (idx >= 0 && idx < fornNome.size()) {
                    fornNome.remove(idx);
                    fornCnpj.remove(idx);
                    fornUf.remove(idx);
                    fornProdutoFornecido.remove(idx);
                    fornPrecoPago.remove(idx);
                    fornLucroPretendidoPct.remove(idx);
                    System.out.println("✅ Sucesso! Fornecedor removido.\n");
                } else {
                    System.out.println("❌ [Erro]: Fornecedor não encontrado.\n");
                }
            }
        }
    }

    /* =========================================================================
       [MÓDULO] FINANCEIRO, ESTOQUE, TRIBUTOS E CENTRAL DE PAGAMENTOS
       ========================================================================= */
    public static void menuFinanceiroEOperacional(Scanner scanner, List<String> prodNome, List<String> prodNcm, List<Double> prodPrecos, 
                                                  List<Double> prodPrecoOriginal, List<Integer> prodEstoque, 
                                                  List<String> funcNome, List<String> funcCargo, List<Double> funcSalario, List<Boolean> funcPago,
                                                  List<String> fornNome, List<String> fornProdutoFornecido, List<Double> fornPrecoPago, List<Boolean> fornPago,
                                                  double saldoCaixaLoja, double totalGastosEstoque, double totalReceitaVendas, 
                                                  double totalSalariosPagos, double totalFornecedoresPagos, double impostosPendentesTotal, 
                                                  int anoExercicioPadrao, String ufOrigem) {
        boolean rodando = true;
        while (rodando) {
            System.out.println("\n================================================================");
            System.out.println("             PAINEL: FINANCEIRO, ESTOQUE E TRIBUTOS             ");
            System.out.println("================================================================");
            System.out.println("[1] Listar produtos e fichas fiscais");
            System.out.println("[2] Cadastrar novo produto e ficha fiscal");
            System.out.println("[3] Ajustar preços de produtos");
            System.out.println("[4] Registrar movimentação de estoque / compras");
            System.out.println("[5] Configurar ano fiscal dos tributos");
            System.out.println("[6] Exibir balanço financeiro e demonstrativo DRE");
            System.out.println("[7] Central de Pagamentos (Salários, Fornecedores e Impostos)");
            System.out.println("[8] Excluir produto do catálogo");
            System.out.println("[9] Voltar ao menu administrativo anterior");
            System.out.println("================================================================");

            int op = lerInteiroValido(scanner, "Selecione uma opção (0 para voltar): ");

            if (op == 9 || op == -999) {
                rodando = false;
            } else if (op == 1) {
                System.out.println("\n================================================================");
                System.out.println("                  CATÁLOGO DE PRODUTOS E TRIBUTOS               ");
                System.out.println("================================================================");
                for (int i = 0; i < prodNome.size(); i++) {
                    if (prodNome.get(i) != null) {
                        System.out.printf("[%d] Produto: %s | Preço: %s | Estoque: %d | NCM: %s\n",
                                (i + 1), prodNome.get(i), formatarMoeda(prodPrecos.get(i)), prodEstoque.get(i), prodNcm.get(i));
                    }
                }
                System.out.println("================================================================");
            } else if (op == 2) {
                try {
                    int idPr = lerInteiroValido(scanner, "Informe o ID/Posição desejada para o produto (0 para voltar): ") - 1;
                    if (idPr == -1000) continue;

                    if (idPr >= 0) {
                        System.out.print("Nome do produto [Ex: Vitamina C] (0 para voltar): ");
                        String nProd = scanner.nextLine().trim().toUpperCase();
                        if (nProd.equals("0") || nProd.equalsIgnoreCase("SAIR")) continue;

                        if (prodNome.contains(nProd)) {
                            System.out.println("❌ [Erro]: Produto já cadastrado.\n");
                            continue;
                        }

                        double pVenda = lerDoubleValido(scanner, "Preço de venda (R$): ");
                        if (pVenda == -999.0) continue;

                        int eIni = lerInteiroValido(scanner, "Quantidade em estoque inicial (0 para voltar): ");
                        if (eIni == -999) continue;

                        System.out.print("Código NCM [Ex: 2106.90.30] (0 para voltar): "); 
                        String ncmVal = scanner.nextLine().trim();
                        if (ncmVal.equals("0") || ncmVal.equalsIgnoreCase("SAIR")) continue;

                        while (prodNome.size() <= idPr) {
                            prodNome.add(null);
                            prodNcm.add("");
                            prodPrecos.add(0.0);
                            prodPrecoOriginal.add(0.0);
                            prodEstoque.add(0);
                        }

                        prodNome.set(idPr, nProd);
                        prodNcm.set(idPr, ncmVal);
                        prodPrecos.set(idPr, pVenda);
                        prodPrecoOriginal.set(idPr, pVenda);
                        prodEstoque.set(idPr, eIni);
                        System.out.println("✅ Sucesso! Produto cadastrado.\n");
                    }
                } catch (Exception e) {
                    System.out.println("❌ [Erro]: Falha ao cadastrar.\n");
                }
            } else if (op == 3) {
                if (!prodNome.isEmpty()) {
                    System.out.println("\n================================================================");
                    System.out.println("                  TABELA DE PREÇOS ATUAIS                       ");
                    System.out.println("================================================================");
                    for (int i = 0; i < prodNome.size(); i++) {
                        if (prodNome.get(i) != null) {
                            System.out.println("ID: " + (i + 1) + " | " + prodNome.get(i) + " | Preço: " + formatarMoeda(prodPrecos.get(i)));
                        }
                    }
                    System.out.println("================================================================");
                    int idPr = lerInteiroValido(scanner, "Informe o ID do produto para alterar preço (0 para voltar): ") - 1;
                    if (idPr == -1000) continue;
                    if (idPr >= 0 && idPr < prodNome.size() && prodNome.get(idPr) != null) {
                        double nP = lerDoubleValido(scanner, "Informe o novo preço fixo (R$): ");
                        if (nP == -999.0) continue;
                        prodPrecos.set(idPr, nP);
                        System.out.println("✅ Sucesso! Preço alterado.\n");
                    }
                }
            } else if (op == 4) {
                for (int i = 0; i < prodNome.size(); i++) {
                    if (prodNome.get(i) != null) {
                        System.out.println("ID: " + (i + 1) + " | " + prodNome.get(i) + " | Estoque atual: " + prodEstoque.get(i));
                    }
                }
                int pI = lerInteiroValido(scanner, "\nInforme o ID do produto para reposição (0 para voltar): ") - 1;
                if (pI == -1000) continue;
                if (pI >= 0 && pI < prodNome.size() && prodNome.get(pI) != null) {
                    int qE = lerInteiroValido(scanner, "Quantidade de entrada no estoque (0 para voltar): ");
                    if (qE == -999) continue;
                    double cT = lerDoubleValido(scanner, "Custo total pago pelo lote (R$): ");
                    if (cT == -999.0) continue;
                    prodEstoque.set(pI, prodEstoque.get(pI) + qE);
                    System.out.println("✅ Sucesso! Estoque abastecido.\n");
                }
            } else if (op == 5) {
                int novoAno = lerInteiroValido(scanner, "Informe o novo ano fiscal de referência [Ex: 2027] (0 para voltar): ");
                if (novoAno != -999) {
                    anoExercicioPadrao = novoAno;
                    System.out.println("✅ Sucesso! Ano fiscal alterado para " + anoExercicioPadrao + ".\n");
                }
            } else if (op == 6) {
                double lucroBruto = totalReceitaVendas - totalGastosEstoque;
                double lucroOperacional = lucroBruto - totalSalariosPagos;
                double impostosSobreLucro = (lucroOperacional > 0) ? (lucroOperacional * 0.15) : 0.0;
                double lucroLiquidoFinal = lucroOperacional - impostosSobreLucro;

                System.out.println("\n================================================================");
                System.out.println("            DEMONSTRATIVO DO RESULTADO DO EXERCÍCIO (DRE)       ");
                System.out.println("================================================================");
                System.out.println(" (★) Saldo atual em caixa: " + formatarMoeda(saldoCaixaLoja));
                System.out.println(" (+) Receita bruta de vendas: " + formatarMoeda(totalReceitaVendas));
                System.out.println(" (-) Custos totais de estoque: " + formatarMoeda(totalGastosEstoque));
                System.out.println("----------------------------------------------------------------");
                System.out.println(" (=) Lucro Bruto: " + formatarMoeda(lucroBruto));
                System.out.println(" (-) Gastos com folha salarial: " + formatarMoeda(totalSalariosPagos));
                System.out.println("----------------------------------------------------------------");
                System.out.println(" (=) Lucro Operacional: " + formatarMoeda(lucroOperacional));
                System.out.println(" (-) Provisão de Impostos (~15%): " + formatarMoeda(impostosSobreLucro));
                System.out.println("----------------------------------------------------------------");
                System.out.println(" (★) LUCRO LÍQUIDO FINAL: " + formatarMoeda(lucroLiquidoFinal));
                System.out.println("================================================================");
            } else if (op == 7) {
                boolean pagando = true;
                while (pagando) {
                    System.out.println("\n================================================================");
                    System.out.println("         CENTRAL DE PAGAMENTOS E OBRIGAÇÕES FINANCEIRAS         ");
                    System.out.println("================================================================");
                    System.out.println(" (★) Saldo atual em caixa: " + formatarMoeda(saldoCaixaLoja));
                    
                    double folhaPend = 0.0;
                    for (int i = 0; i < funcSalario.size(); i++) {
                        if (!funcPago.get(i)) folhaPend += funcSalario.get(i);
                    }
                    double fornPend = 0.0;
                    for (int i = 0; i < fornPrecoPago.size(); i++) {
                        if (!fornPago.get(i)) fornPend += fornPrecoPago.get(i);
                    }

                    System.out.println(" 🔴 Total de Salários Pendentes: " + formatarMoeda(folhaPend));
                    System.out.println(" 🔴 Total de Fornecedores Pendentes: " + formatarMoeda(fornPend));
                    System.out.println(" 🔴 Total de Impostos Pendentes: " + formatarMoeda(impostosPendentesTotal));
                    System.out.println("----------------------------------------------------------------");
                    System.out.println("[1] Pagar Salários (Listar, Pagar Específico ou Todos)");
                    System.out.println("[2] Pagar Fornecedores (Listar, Pagar Específico ou Todos)");
                    System.out.println("[3] Pagar Impostos da Receita Federal");
                    System.out.println("[4] Voltar ao menu financeiro anterior");
                    System.out.println("================================================================");

                    int opPag = lerInteiroValido(scanner, "Selecione uma opção (0 para voltar): ");

                    if (opPag == 4 || opPag == -999) {
                        pagando = false;
                    } else if (opPag == 1) {
                        System.out.println("\n================================================================");
                        System.out.println("                   PAGAMENTO DE SALÁRIOS                        ");
                        System.out.println("================================================================");
                        for (int i = 0; i < funcNome.size(); i++) {
                            String st = funcPago.get(i) ? "PAGO ✅" : "PENDENTE ❌";
                            System.out.printf("[%d] %s (%s) - Valor: %s [%s]\n", (i + 1), funcNome.get(i), funcCargo.get(i), formatarMoeda(funcSalario.get(i)), st);
                        }
                        System.out.println("================================================================");
                        System.out.println("[1] Pagar funcionário específico | [2] Pagar todos de uma vez | [3] Voltar");
                        int subS = lerInteiroValido(scanner, "Escolha uma opção (0 para voltar): ");
                        if (subS == -999 || subS == 3) continue;
                        
                        if (subS == 1) {
                            int idxF = lerInteiroValido(scanner, "Informe o número (ID) do funcionário (0 para voltar): ") - 1;
                            if (idxF == -1000) continue;
                            if (idxF >= 0 && idxF < funcNome.size()) {
                                if (funcPago.get(idxF)) {
                                    System.out.println("ℹ️ Este salário já foi quitado.");
                                } else {
                                    double val = funcSalario.get(idxF);
                                    if (saldoCaixaLoja >= val) {
                                        saldoCaixaLoja -= val;
                                        totalSalariosPagos += val;
                                        funcPago.set(idxF, true);
                                        System.out.println("✅ Salário de " + funcNome.get(idxF) + " pago com sucesso!");
                                    } else {
                                        System.out.println("❌ [Erro]: Caixa insuficiente.");
                                    }
                                }
                            } else {
                                System.out.println("❌ [Erro]: Funcionário inválido.");
                            }
                        } else if (subS == 2) {
                            if (folhaPend <= 0) {
                                System.out.println("ℹ️ Não há salários pendentes.");
                            } else if (saldoCaixaLoja >= folhaPend) {
                                saldoCaixaLoja -= folhaPend;
                                totalSalariosPagos += folhaPend;
                                for (int i = 0; i < funcPago.size(); i++) {
                                    funcPago.set(i, true);
                                }
                                System.out.println("✅ Todos os salários pendentes foram quitados com sucesso!");
                            } else {
                                System.out.println("❌ [Erro]: Caixa insuficiente para quitar a folha completa.");
                            }
                        }
                    } else if (opPag == 2) {
                        System.out.println("\n================================================================");
                        System.out.println("                 PAGAMENTO DE FORNECEDORES                      ");
                        System.out.println("================================================================");
                        for (int i = 0; i < fornNome.size(); i++) {
                            String st = fornPago.get(i) ? "PAGO ✅" : "PENDENTE ❌";
                            System.out.printf("[%d] %s - Produto: %s - Valor Custo: %s [%s]\n", (i + 1), fornNome.get(i), fornProdutoFornecido.get(i), formatarMoeda(fornPrecoPago.get(i)), st);
                        }
                        System.out.println("================================================================");
                        System.out.println("[1] Pagar fornecedor específico | [2] Pagar todos de uma vez | [3] Voltar");
                        int subF = lerInteiroValido(scanner, "Escolha uma opção (0 para voltar): ");
                        if (subF == -999 || subF == 3) continue;

                        if (subF == 1) {
                            int idxFor = lerInteiroValido(scanner, "Informe o número (ID) do fornecedor (0 para voltar): ") - 1;
                            if (idxFor == -1000) continue;
                            if (idxFor >= 0 && idxFor < fornNome.size()) {
                                if (fornPago.get(idxFor)) {
                                    System.out.println("ℹ️ Este fornecedor já foi pago.");
                                } else {
                                    double valF = fornPrecoPago.get(idxFor);
                                    if (saldoCaixaLoja >= valF) {
                                        saldoCaixaLoja -= valF;
                                        totalFornecedoresPagos += valF;
                                        fornPago.set(idxFor, true);
                                        System.out.println("✅ Fornecedor " + fornNome.get(idxFor) + " pago com sucesso!");
                                    } else {
                                        System.out.println("❌ [Erro]: Caixa insuficiente.");
                                    }
                                }
                            } else {
                                System.out.println("❌ [Erro]: Fornecedor inválido.");
                            }
                        } else if (subF == 2) {
                            if (fornPend <= 0) {
                                System.out.println("ℹ️ Não há fornecedores pendentes.");
                            } else if (saldoCaixaLoja >= fornPend) {
                                saldoCaixaLoja -= fornPend;
                                totalFornecedoresPagos += fornPend;
                                for (int i = 0; i < fornPago.size(); i++) {
                                    fornPago.set(i, true);
                                }
                                System.out.println("✅ Todos os fornecedores pendentes foram pagos com sucesso!");
                            } else {
                                System.out.println("❌ [Erro]: Caixa insuficiente para quitar todos os fornecedores.");
                            }
                        }
                    } else if (opPag == 3) {
                        System.out.println("\n--- QUITAÇÃO DE IMPOSTOS ---");
                        System.out.println("Total de Impostos Pendentes: " + formatarMoeda(impostosPendentesTotal));
                        if (impostosPendentesTotal <= 0) {
                            System.out.println("ℹ️ Não há impostos pendentes para pagamento.");
                        } else {
                            System.out.print("Deseja quitar os impostos agora? [S/N] (0 para voltar): ");
                            String respImp = scanner.nextLine().trim();
                            if (respImp.equalsIgnoreCase("0") || respImp.equalsIgnoreCase("SAIR")) continue;
                            if (respImp.equalsIgnoreCase("S")) {
                                if (saldoCaixaLoja >= impostosPendentesTotal) {
                                    saldoCaixaLoja -= impostosPendentesTotal;
                                    impostosPendentesTotal = 0.0;
                                    System.out.println("✅ Impostos quitados com sucesso junto à Receita Federal!");
                                } else {
                                    System.out.println("❌ [Erro]: Caixa insuficiente para quitar os impostos.");
                                }
                            }
                        }
                    }
                }
            } else if (op == 8) {
                System.out.println("\n--- LISTA DE PRODUTOS PARA EXCLUSÃO ---");
                for (int i = 0; i < prodNome.size(); i++) {
                    if (prodNome.get(i) != null) {
                        System.out.printf("[%d] %s\n", (i + 1), prodNome.get(i));
                    }
                }
                int idx = lerInteiroValido(scanner, "\nInforme o número (ID) do produto que deseja excluir (0 para voltar): ") - 1;
                if (idx == -1000) continue;
                if (idx >= 0 && idx < prodNome.size() && prodNome.get(idx) != null) {
                    prodNome.set(idx, null);
                    prodNcm.set(idx, "");
                    prodPrecos.set(idx, 0.0);
                    prodEstoque.set(idx, 0);
                    System.out.println("✅ Sucesso! Produto removido do catálogo.\n");
                } else {
                    System.out.println("❌ [Erro]: Produto não encontrado.\n");
                }
            }
        }
    }

    /* =========================================================================
       MÉTODO PRINCIPAL (MAIN)
       ========================================================================= */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        List<String> funcId = new ArrayList<>(Arrays.asList("F01", "F02", "F03"));
        List<String> funcNome = new ArrayList<>(Arrays.asList("CARLOS GERENTE", "ANA OPERADORA", "MARINA FINANCEIRO"));
        List<String> funcCpf = new ArrayList<>(Arrays.asList("80695789554", "89246912950", "52998224725"));
        List<String> funcCargo = new ArrayList<>(Arrays.asList("GERENTE", "OPERADOR", "FINANCEIRO"));
        List<String> funcSenha = new ArrayList<>(Arrays.asList("admin123", "op123", "fin123"));
        List<Double> funcSalario = new ArrayList<>(Arrays.asList(5500.00, 2200.00, 4800.00));
        List<Boolean> funcPago = new ArrayList<>(Arrays.asList(false, false, false));

        List<String> cliNomeArr = new ArrayList<>(Arrays.asList("MARIO SILVA"));
        List<String> cliCpfArr = new ArrayList<>(Arrays.asList("11144477735"));
        List<String> cliSenhaArr = new ArrayList<>(Arrays.asList("cli123"));
        List<String> cliSexoArr = new ArrayList<>(Arrays.asList("M"));
        List<String> cliDataNascArr = new ArrayList<>(Arrays.asList("10/05/1995"));
        List<String> cliUfArr = new ArrayList<>(Arrays.asList("SP"));
        List<String> cliCepArr = new ArrayList<>(Arrays.asList("04753060"));

        List<String> prodNome = new ArrayList<>(Arrays.asList(
            "WHEY PROTEIN ISOLADO", "BEBIDA ISOTÔNICA 500ML", "CAMISA ESPORTIVA DRY-FIT", 
            "TÊNIS DE CORRIDA PERFORMANCE", "CREATINA MONOHIDRATADA 250G", "BARRA DE PROTEÍNA CHOCOLATE", 
            "ENERGÉTICO LATA 473ML", "ÁGUA MINERAL 500ML SEM GÁS", "BERMUDA TÉRMICA COMPRESSÃO", 
            "MEIAS ESPORTIVAS CANO ALTO (3UN)", "CHINELO SLIDE ANATÔMICO", "MOCHILA ESPORTIVA IMPERMEÁVEL", 
            "GARRAFA SQUEEZE 1L INOX", "LUVA DE TREINO MUSCULAÇÃO"
        ));
        List<String> prodNcm = new ArrayList<>(Arrays.asList(
            "2106.90.30", "2202.99.00", "6109.90.00", "6404.11.00", "2106.90.90", 
            "2106.90.30", "2202.99.00", "2201.10.00", "6112.19.00", "6115.96.00", 
            "6402.20.00", "4202.92.00", "7323.93.00", "4203.29.00"
        ));
        List<Double> prodPrecos = new ArrayList<>(Arrays.asList(
            120.00, 8.50, 59.90, 249.90, 89.90, 9.00, 11.00, 3.50, 79.90, 34.90, 69.90, 159.90, 49.90, 45.00
        ));
        List<Double> prodPrecoOriginal = new ArrayList<>(Arrays.asList(
            120.00, 10.00, 59.90, 299.90, 89.90, 10.00, 11.00, 3.50, 89.90, 34.90, 69.90, 179.90, 49.90, 50.00
        ));
        List<Integer> prodEstoque = new ArrayList<>(Arrays.asList(
            50, 100, 30, 15, 40, 120, 80, 200, 25, 50, 30, 20, 45, 35
        ));

        List<String> fornNome = new ArrayList<>(Arrays.asList("SUPLEMAX DISTRIBUIDORA LTDA"));
        List<String> fornCnpj = new ArrayList<>(Arrays.asList("12345678000190"));
        List<String> fornUf = new ArrayList<>(Arrays.asList("SP"));
        List<String> fornProdutoFornecido = new ArrayList<>(Arrays.asList("WHEY PROTEIN ISOLADO"));
        List<Double> fornPrecoPago = new ArrayList<>(Arrays.asList(80.00));
        List<Double> fornLucroPretendidoPct = new ArrayList<>(Arrays.asList(50.0));
        List<Boolean> fornPago = new ArrayList<>(Arrays.asList(false));

        double saldoCaixaLoja = 100000.00;   
        double totalGastosEstoque = 15000.00; 
        double totalReceitaVendas = 0.0;     
        double totalSalariosPagos = 0.0;     
        double totalFornecedoresPagos = 0.0;
        double impostosPendentesTotal = 1250.00; 

        String ufOrigem = "SP";
        int anoExercicioPadrao = 2026;

        boolean sistemaAtivo = true;
        while (sistemaAtivo) {
            exibirCabecalho();
            System.out.println("SELECIONE O SEU PERFIL DE ACESSO:");
            System.out.println("[1] CLIENTE (Catálogo de compras e carrinho)");
            System.out.println("[2] FUNCIONÁRIO (Painel administrativo e login)");
            System.out.println("[3] SAIR DO SISTEMA");

            int perfilAcesso = lerInteiroValido(scanner, "\nDigite a opção desejada: ");

            if (perfilAcesso == 3 || perfilAcesso == -999) {
                sistemaAtivo = false;
                break;
            } else if (perfilAcesso < 1 || perfilAcesso > 2) {
                System.out.println("❌ [Erro]: Opção inválida.\n");
                continue;
            }

            /* =========================================================================
               FLUXO DO CLIENTE
               ========================================================================= */
            if (perfilAcesso == 1) {
                String cliNome = "";
                String cliCpf = "";
                String cliUf = "SP";
                String cliCep = "04753060";
                boolean clienteIdentificado = false;
                boolean clienteCadastrado = false;

                List<Integer> carrinhoProdIndex = new ArrayList<>();
                List<Integer> carrinhoQtde = new ArrayList<>();

                boolean loopCliente = true;
                while (loopCliente) {
                    System.out.println("\n================================================================");
                    System.out.println("                    PAINEL DO CLIENTE                           ");
                    System.out.println("================================================================");
                    System.out.println("[1] Visualizar catálogo de produtos");
                    System.out.println("[2] Adicionar produto ao carrinho de compras");
                    System.out.println("[3] Visualizar carrinho / Revisar / Finalizar compra");
                    System.out.println("[4] Voltar à página anterior (Trocar perfil)");
                    System.out.println("[5] Voltar ao início do sistema");
                    System.out.println("================================================================");

                    int opcaoCliente = lerInteiroValido(scanner, "Selecione uma opção (0 para voltar): ");

                    if (opcaoCliente == 4 || opcaoCliente == -999) {
                        break; 
                    } else if (opcaoCliente == 5) {
                        loopCliente = false;
                        continue; 
                    }

                    if ((opcaoCliente == 2 || opcaoCliente == 3) && !clienteIdentificado) {
                        System.out.println("\n================================================================");
                        System.out.println("                   IDENTIFICAÇÃO DO CLIENTE                     ");
                        System.out.println("================================================================");
                        cliNome = lerTextoApenasLetras(scanner, "Nome completo [Ex: Maria Souza] (0 para voltar): ");
                        if (cliNome.equals("CANCELAR")) continue;

                        cliCpf = lerCpfValido(scanner);
                        if (cliCpf.equals("CANCELAR")) continue;
                        System.out.println("================================================================");

                        int clienteIdx = cliCpfArr.indexOf(cliCpf);

                        if (clienteIdx != -1) {
                            System.out.println("ℹ️  CPF já cadastrado (" + cliNomeArr.get(clienteIdx) + ").");
                            boolean senhaCorreta = false;
                            int tentativasSenha = 3;

                            while (tentativasSenha > 0 && !senhaCorreta) {
                                System.out.print("Digite sua senha de acesso (0 para voltar): ");
                                String senhaDigitada = scanner.nextLine().trim();

                                if (senhaDigitada.equals("0") || senhaDigitada.equalsIgnoreCase("SAIR")) {
                                    senhaCorreta = false;
                                    break;
                                }

                                if (cliSenhaArr.get(clienteIdx).equals(senhaDigitada)) {
                                    senhaCorreta = true;
                                    cliNome = cliNomeArr.get(clienteIdx);
                                    cliUf = cliUfArr.get(clienteIdx);
                                    cliCep = cliCepArr.get(clienteIdx);
                                    clienteCadastrado = true;
                                    System.out.println("✅ Senha correta! Acesso liberado.");
                                } else {
                                    tentativasSenha--;
                                    if (tentativasSenha > 0) {
                                        System.out.println("❌ Senha incorreta. Restam " + tentativasSenha + " tentativa(s).");
                                    } else {
                                        System.out.println("❌ Limite de tentativas excedido.\n");
                                    }
                                }
                            }
                            if (!senhaCorreta) continue;
                        } else {
                            int optCliNovo = lerInteiroValido(scanner, "CPF não cadastrado. [1 - Cadastrar agora | 2 - Comprar sem cadastro] (0 para voltar): ");
                            if (optCliNovo == -999) continue;

                            if (optCliNovo == 1) {
                                System.out.println("\n================================================================");
                                System.out.println("                 CADASTRO RÁPIDO DO CLIENTE                     ");
                                System.out.println("================================================================");
                                String sexoCli = lerTextoApenasLetras(scanner, "Sexo [Ex: M ou F] (0 para voltar): ");
                                if (sexoCli.equals("CANCELAR")) continue;

                                System.out.print("Data de nascimento [Ex: 10/05/1995] (0 para voltar): ");
                                String nascCli = scanner.nextLine().trim();
                                if (nascCli.equals("0") || nascCli.equalsIgnoreCase("SAIR")) continue;

                                cliUf = lerUfValida(scanner, "UF de residência [Ex: SP]: ");
                                if (cliUf.equals("CANCELAR")) continue;

                                cliCep = lerCepValido(scanner);
                                if (cliCep.equals("CANCELAR")) continue;

                                String cliSenha = geradorSenhaAleatoriaSistema();
                                System.out.println("================================================================");
                                System.out.println("🔑 Senha gerada para futuros acessos: " + cliSenha);

                                cliNomeArr.add(cliNome);
                                cliCpfArr.add(cliCpf);
                                cliSenhaArr.add(cliSenha);
                                cliSexoArr.add(sexoCli);
                                cliDataNascArr.add(nascCli.isEmpty() ? "01/01/2000" : nascCli);
                                cliUfArr.add(cliUf);
                                cliCepArr.add(cliCep);
                                clienteCadastrado = true;
                            } else {
                                clienteCadastrado = false;
                            }
                        }
                        clienteIdentificado = true;
                        System.out.println("\n👋 Bem-vindo(a), " + cliNome.toUpperCase() + "!");
                    }

                    if (opcaoCliente == 1) {
                        System.out.println("\n================================================================");
                        System.out.println("                  CATÁLOGO DE PRODUTOS DISPONÍVEIS              ");
                        System.out.println("================================================================");
                        for (int i = 0; i < prodNome.size(); i++) {
                            if (prodNome.get(i) != null) {
                                String statusEstoque = (prodEstoque.get(i) > 0) ? " | Estoque: " + prodEstoque.get(i) : " | [ESGOTADO]";
                                String tagOferta = (prodPrecos.get(i) < prodPrecoOriginal.get(i)) ? " ** [OFERTA] **" : "";
                                System.out.println("ID: " + (i + 1) + " | " + prodNome.get(i) + " | Preço: " + formatarMoeda(prodPrecos.get(i)) + tagOferta + statusEstoque);
                            }
                        }
                        System.out.println("================================================================");
                    } else if (opcaoCliente == 2) {
                        System.out.println("\n================================================================");
                        System.out.println("               ADICIONAR PRODUTO AO CARRINHO                    ");
                        System.out.println("================================================================");
                        boolean temProd = false;
                        for (int i = 0; i < prodNome.size(); i++) {
                            if (prodNome.get(i) != null && prodEstoque.get(i) > 0) {
                                System.out.println("ID: " + (i + 1) + " | " + prodNome.get(i) + " - " + formatarMoeda(prodPrecos.get(i)) + " (Disp: " + prodEstoque.get(i) + ")");
                                temProd = true;
                            }
                        }
                        System.out.println("================================================================");

                        if (!temProd) {
                            System.out.println("ℹ️  Nenhum produto disponível em estoque.");
                        } else {
                            int posItem = lerInteiroValido(scanner, "Digite o ID do produto desejado (0 para voltar): ") - 1;
                            if (posItem == -1000) continue;

                            if (posItem >= 0 && posItem < prodNome.size() && prodNome.get(posItem) != null && prodEstoque.get(posItem) > 0) {
                                int qtde = lerInteiroValido(scanner, "Digite a quantidade desejada (0 para voltar): ");
                                if (qtde == -999) continue;

                                if (qtde > 0 && qtde <= prodEstoque.get(posItem)) {
                                    carrinhoProdIndex.add(posItem);
                                    carrinhoQtde.add(qtde);
                                    System.out.println("✅ Produto adicionado ao carrinho!");
                                } else {
                                    System.out.println("❌ [Erro]: Quantidade inválida ou superior ao estoque (" + prodEstoque.get(posItem) + ").");
                                }
                            } else {
                                System.out.println("❌ [Erro]: ID inválido.");
                            }
                        }
                    } else if (opcaoCliente == 3) {
                        if (carrinhoProdIndex.isEmpty()) {
                            System.out.println("\nℹ️  Seu carrinho de compras está vazio.");
                        } else {
                            boolean revisando = true;
                            while (revisando) {
                                double valCarrinho = 0.0;
                                System.out.println("\n================================================================");
                                System.out.println("                 REVISÃO DO CARRINHO DE COMPRAS                 ");
                                System.out.println("================================================================");
                                for (int i = 0; i < carrinhoProdIndex.size(); i++) {
                                    int idxP = carrinhoProdIndex.get(i);
                                    double valItem = prodPrecos.get(idxP) * carrinhoQtde.get(i);
                                    valCarrinho += valItem;
                                    System.out.printf("[%d] %s | Qtde: %d | Unitário: %s | Subtotal: %s\n", 
                                            (i + 1), prodNome.get(idxP), carrinhoQtde.get(i), formatarMoeda(prodPrecos.get(idxP)), formatarMoeda(valItem));
                                }
                                System.out.println("----------------------------------------------------------------");
                                System.out.println("VALOR TOTAL DO CARRINHO: " + formatarMoeda(valCarrinho));
                                System.out.println("================================================================");

                                System.out.println("[1] Concluir compra | [2] Alterar quantidade | [3] Remover item | [4] Voltar");
                                System.out.println("================================================================");
                                int acao = lerInteiroValido(scanner, "Escolha uma opção (0 para voltar): ");
                                if (acao == -999 || acao == 4) {
                                    revisando = false;
                                    continue;
                                }

                                if (acao == 1) {
                                    if (!clienteCadastrado) {
                                        System.out.println("\n================================================================");
                                        System.out.println("             CADASTRO RÁPIDO PARA FATURAMENTO                   ");
                                        System.out.println("================================================================");
                                        String sexoC = lerTextoApenasLetras(scanner, "Sexo [Ex: M] (0 para voltar): ");
                                        if (sexoC.equals("CANCELAR")) continue;

                                        System.out.print("Data de nascimento [Ex: 10/05/1995] (0 para voltar): "); 
                                        String nascC = scanner.nextLine().trim();
                                        if (nascC.equals("0") || nascC.equalsIgnoreCase("SAIR")) continue;

                                        cliUf = lerUfValida(scanner, "UF de residência [Ex: SP]: ");
                                        if (cliUf.equals("CANCELAR")) continue;

                                        cliCep = lerCepValido(scanner);
                                        if (cliCep.equals("CANCELAR")) continue;

                                        String senhaGerada = geradorSenhaAleatoriaSistema();
                                        System.out.println("================================================================");

                                        cliNomeArr.add(cliNome);
                                        cliCpfArr.add(cliCpf);
                                        cliSenhaArr.add(senhaGerada);
                                        cliSexoArr.add(sexoC);
                                        cliDataNascArr.add(nascC.isEmpty() ? "01/01/2000" : nascC);
                                        cliUfArr.add(cliUf);
                                        cliCepArr.add(cliCep);
                                        clienteCadastrado = true;
                                        System.out.println("🔑 Cadastro efetivado! Senha: " + senhaGerada);
                                    }

                                    int pagto = lerInteiroValido(scanner, "Forma de Pagamento [1 - PIX (10% Desc) | 2 - Crédito à Vista | 3 - Parcelado 3x (+5%)] (0 para voltar): ");
                                    if (pagto == -999) continue;

                                    double valFinal = valCarrinho;
                                    if (pagto == 1) valFinal *= 0.90;
                                    else if (pagto == 3) valFinal *= 1.05;

                                    revisando = false;
                                    totalReceitaVendas += valFinal;
                                    saldoCaixaLoja += valFinal;

                                    for (int i = 0; i < carrinhoProdIndex.size(); i++) {
                                        int idxP = carrinhoProdIndex.get(i);
                                        prodEstoque.set(idxP, prodEstoque.get(idxP) - carrinhoQtde.get(i));
                                    }

                                    System.out.println("\n================================================================");
                                    System.out.println("                 NOTA FISCAL EMITIDA COM SUCESSO                ");
                                    System.out.println("================================================================");
                                    System.out.println("Cliente: " + cliNome.toUpperCase());
                                    System.out.println("Valor Final Pago: " + formatarMoeda(valFinal));
                                    System.out.println("================================================================");
                                    carrinhoProdIndex.clear();
                                    carrinhoQtde.clear();
                                } else if (acao == 2) {
                                    int idxA = lerInteiroValido(scanner, "Número do item no carrinho para alterar (0 para voltar): ") - 1;
                                    if (idxA == -1000) continue;
                                    if (idxA >= 0 && idxA < carrinhoProdIndex.size()) {
                                        int nQtde = lerInteiroValido(scanner, "Nova quantidade desejada: ");
                                        if (nQtde == -999) continue;
                                        int idxP = carrinhoProdIndex.get(idxA);
                                        if (nQtde > 0 && nQtde <= prodEstoque.get(idxP)) {
                                            carrinhoQtde.set(idxA, nQtde);
                                            System.out.println("✅ Quantidade atualizada.");
                                        } else {
                                            System.out.println("❌ [Erro]: Estoque insuficiente.");
                                        }
                                    }
                                } else if (acao == 3) {
                                    int idxR = lerInteiroValido(scanner, "Número do item para remover do carrinho (0 para voltar): ") - 1;
                                    if (idxR == -1000) continue;
                                    if (idxR >= 0 && idxR < carrinhoProdIndex.size()) {
                                        carrinhoProdIndex.remove(idxR);
                                        carrinhoQtde.remove(idxR);
                                        System.out.println("✅ Item removido.");
                                    }
                                }
                            }
                        }
                    }
                }

            /* =========================================================================
               FLUXO DO FUNCIONÁRIO (PAINEL ADMINISTRATIVO UNIFICADO)
               ========================================================================= */
            } else if (perfilAcesso == 2) {
                System.out.println("\n----------------------------------------------------------------");
                System.out.println("                   LOGIN DO FUNCIONÁRIO                         ");
                System.out.println("----------------------------------------------------------------");
                String cpfLogin = lerCpfValido(scanner);
                if (cpfLogin.equals("CANCELAR")) continue;

                int funcLogadoIndex = funcCpf.indexOf(cpfLogin);

                if (funcLogadoIndex == -1) {
                    System.out.println("❌ [Erro]: CPF não cadastrado.\n");
                    continue;
                }

                int tentativas = 3;
                boolean acessoLiberado = false;
                while (tentativas > 0) {
                    System.out.print("Digite sua senha de funcionário (0 para voltar): ");
                    String senhaL = scanner.nextLine().trim();

                    if (senhaL.equals("0") || senhaL.equalsIgnoreCase("SAIR")) {
                        break;
                    }

                    if (funcSenha.get(funcLogadoIndex).equals(senhaL)) {
                        acessoLiberado = true;
                        break;
                    } else {
                        tentativas--;
                        System.out.println("❌ Senha incorreta. Restam " + tentativas + " tentativa(s).");
                    }
                }

                if (!acessoLiberado) continue;

                String cargoAtual = funcCargo.get(funcLogadoIndex);
                System.out.println("\n👋 Bem-vindo(a), " + funcNome.get(funcLogadoIndex) + " [" + cargoAtual + "]!");

                boolean loopAdmin = true;
                while (loopAdmin) {
                    System.out.println("\n================================================================");
                    System.out.println("               MENU ADMINISTRATIVO UNIFICADO                    ");
                    System.out.println("================================================================");
                    System.out.println("[1] Gestão de Funcionários (Cadastrar, Listar, Alterar, Excluir)");
                    System.out.println("[2] Gestão de Fornecedores (Cadastrar, Listar, Alterar, Excluir)");
                    System.out.println("[3] Gestão Financeira, Produtos, Estoque e Central de Pagamentos");
                    System.out.println("[4] Emitir nota fiscal de saída");
                    System.out.println("[5] Alterar minha senha de acesso");
                    System.out.println("[6] Voltar à página anterior (Trocar perfil)");
                    System.out.println("[7] Voltar ao início do sistema");
                    System.out.println("================================================================");

                    int opcaoMenu = lerInteiroValido(scanner, "Selecione uma opção (0 para voltar): ");

                    if (opcaoMenu == 6 || opcaoMenu == -999) {
                        break; 
                    } else if (opcaoMenu == 7) {
                        loopAdmin = false;
                        continue; 
                    }

                    if (opcaoMenu == 1 && !cargoAtual.equals("GERENTE")) {
                        System.out.println("\n⛔ [Acesso Negado]: Apenas gerentes gerenciam funcionários.");
                        continue;
                    }
                    if (opcaoMenu == 3 && cargoAtual.equals("OPERADOR")) {
                        System.out.println("\n⛔ [Acesso Negado]: Operadores não acessam o painel financeiro.");
                        continue;
                    }

                    if (opcaoMenu == 1) {
                        menuGerenciarFuncionarios(scanner, funcId, funcNome, funcCpf, funcCargo, funcSenha, funcSalario, funcPago);
                    } else if (opcaoMenu == 2) {
                        menuGerenciarFornecedores(scanner, fornNome, fornCnpj, fornUf, fornProdutoFornecido, fornPrecoPago, fornLucroPretendidoPct);
                    } else if (opcaoMenu == 3) {
                        menuFinanceiroEOperacional(scanner, prodNome, prodNcm, prodPrecos, prodPrecoOriginal, prodEstoque, 
                                                   funcNome, funcCargo, funcSalario, funcPago, fornNome, fornProdutoFornecido, fornPrecoPago, fornPago,
                                                   saldoCaixaLoja, totalGastosEstoque, totalReceitaVendas, totalSalariosPagos, 
                                                   totalFornecedoresPagos, impostosPendentesTotal, anoExercicioPadrao, ufOrigem);
                    } else if (opcaoMenu == 4) {
                        String ufD = lerUfValida(scanner, "Informe a UF de destino [Ex: RJ]: ");
                        if (ufD.equals("CANCELAR")) continue;
                        double tN = 0, tI = 0;
                        for (int i = 0; i < prodNome.size(); i++) {
                            if (prodNome.get(i) != null) {
                                double vI = prodPrecos.get(i) * prodEstoque.get(i);
                                tN += vI;
                                tI += calcularImpostoItem(vI, buscarAliquotaIcms(prodNcm.get(i), ufOrigem, ufD, anoExercicioPadrao));
                            }
                        }
                        System.out.println("\n--- PRÉVIA DA NOTA FISCAL DE SAÍDA ---");
                        System.out.println("Valor Bruto Total: " + formatarMoeda(tN));
                        System.out.println("Total de Tributos Calculados: " + formatarMoeda(tI));
                    } else if (opcaoMenu == 5) {
                        System.out.print("Digite sua senha atual (0 para voltar): ");
                        String sA = scanner.nextLine().trim();
                        if (sA.equals("0") || sA.equalsIgnoreCase("SAIR")) continue;
                        if (funcSenha.get(funcLogadoIndex).equals(sA)) {
                            System.out.print("Digite a nova senha: ");
                            String nS = scanner.nextLine().trim();
                            if (!nS.isEmpty()) {
                                funcSenha.set(funcLogadoIndex, nS);
                                System.out.println("✅ Sucesso! Senha alterada.");
                            }
                        } else {
                            System.out.println("❌ [Erro]: Senha incorreta.");
                        }
                    }
                }
            }
        }
        System.out.println("\n===============================================================");
        System.out.println("          Obrigado por usar o Protei Creat                      ");
        System.out.println("                  SESSÃO ENCERRADA.                             ");
        System.out.println("================================================================");
        scanner.close();
    }
}