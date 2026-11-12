import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class prototo {

    /* =========================================================================
       [MÓDULO 1] TRIBUTÁRIO E DE MATRIZ FISCAL (RECEITA FEDERAL / 2026-2027)
       ========================================================================= */
    public static double aliquotaIcmsInterno2026 = 0.18;
    public static double aliquotaIcmsInterno2027 = 0.19; 

    /**
     * [Função: buscarAliquotaIcms]
     * Objetivo: Retorna a alíquota de ICMS aplicável para a operação comercial.
     */
    public static double buscarAliquotaIcms(String ncm, String ufOrigem, String ufDestino, int anoExercicio) {
        double aliquotaBase = (anoExercicio == 2027) ? aliquotaIcmsInterno2027 : aliquotaIcmsInterno2026;
        if (ufOrigem.equalsIgnoreCase("SP") && ufDestino.equalsIgnoreCase("SP")) {
            return aliquotaBase; 
        } else {
            return 0.12; 
        }
    }

    /* =========================================================================
       [MÓDULO 2] UTILITÁRIOS
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
       [MÓDULO 3] VALIDAÇÃO RIGOROSA E ANÁLISE DE CARACTERES POR CARACTERE
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
                    System.out.println("💡 [Dica de Emergência]: Digite '0' ou 'SAIR' para cancelar e voltar ao menu anterior.\n");
                    continue;
                }

                boolean caracteresValidos = true;
                for (int i = 0; i < entrada.length(); i++) {
                    char c = entrada.charAt(i);
                    String sCaractere = String.valueOf(c);
                    if (!sCaractere.matches("^[a-zA-ZáàâãéèêíïóôõúüçÁÀÂÃÉÈÊÍÏÓÔÕÚÜÇ\\s]$")) {
                        caracteresValidos = false;
                        break;
                    }
                }

                if (!caracteresValidos) {
                    System.out.println("❌ [Erro]: O texto contém caracteres ou símbolos inválidos. Aceita apenas letras e acentuação.");
                    System.out.println("💡 [Dica de Emergência]: Digite '0' ou 'SAIR' para cancelar e voltar ao menu anterior.\n");
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
                    System.out.println("💡 [Dica de Emergência]: Digite '0' ou 'SAIR' para cancelar e voltar ao menu anterior.\n");
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
                    return -999;
                }

                if (!entrada.matches("^[0-9]+$")) {
                    System.out.println("❌ [Erro]: Digite apenas números inteiros positivos válidos.");
                    System.out.println("💡 [Dica de Emergência]: Digite '0' ou 'SAIR' para cancelar e voltar ao menu anterior.\n");
                    continue;
                }
                valor = Integer.parseInt(entrada);
                break;
            } catch (Exception e) {
                System.out.println("❌ [Erro]: Entrada inválida. Digite apenas números.");
                System.out.println("💡 [Dica de Emergência]: Digite '0' ou 'SAIR' para cancelar e voltar ao menu anterior.\n");
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
                
                if (entrada.equalsIgnoreCase("SAIR")) {
                    return -999.0;
                }

                valor = Double.parseDouble(entrada);
                if (!Double.isFinite(valor) || valor < 0) {
                    System.out.println("❌ [Erro]: O valor deve ser um número finito e não negativo.");
                    System.out.println("💡 [Dica de Emergência]: Digite 'SAIR' para cancelar e voltar ao menu anterior.\n");
                    continue;
                }
                break;
            } catch (Exception e) {
                System.out.println("❌ [Erro]: Formato numérico inválido (exemplo correto: 49.90).");
                System.out.println("💡 [Dica de Emergência]: Digite 'SAIR' para cancelar e voltar ao menu anterior.\n");
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
                    System.out.println("💡 [Dica de Emergência]: Digite '0' ou 'SAIR' para cancelar e voltar ao menu anterior.\n");
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
                    System.out.println("❌ [Erro]: Dígitos verificadores do CPF incorretos.\n");
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
                    System.out.println("💡 [Dica de Emergência]: Digite '0' ou 'SAIR' para cancelar e voltar ao menu anterior.\n");
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
                    System.out.println("❌ [Erro]: Dígitos verificadores do CNPJ incorretos.\n");
                }
            } catch (Exception e) {
                System.out.println("❌ [Erro]: Falha ao validar o CNPJ.\n");
            }
        }
        return cnpj;
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
       [MÓDULO] GESTÃO DE FUNCIONÁRIOS
       ========================================================================= */
    public static void menuGerenciarFuncionarios(Scanner scanner, List<String> funcId, List<String> funcNome, List<String> funcCpf, 
                                                   List<String> funcCargo, List<String> funcSenha, List<Double> funcSalario, 
                                                   List<Boolean> funcPago) {
        boolean rodando = true;
        while (rodando) {
            System.out.println("\n================================================================");
            System.out.println("                    PAINEL: GESTÃO DE FUNCIONÁRIOS              ");
            System.out.println("================================================================");
            System.out.println("[1] Listar funcionários cadastrados");
            System.out.println("[2] Cadastrar novo funcionário");
            System.out.println("[3] Alterar salário de um funcionário");
            System.out.println("[4] Excluir funcionário");
            System.out.println("[5] Voltar ao menu anterior");
            System.out.println("================================================================");

            int op = lerInteiroValido(scanner, "Selecione uma opção [0 ou SAIR para voltar]: ");

            if (op == 5 || op == -999) {
                rodando = false;
            } else if (op == 1) {
                System.out.println("\n================================================================");
                System.out.println("                  LISTA DE FUNCIONÁRIOS ATIVOS                  ");
                System.out.println("================================================================");
                for (int i = 0; i < funcNome.size(); i++) {
                    String statusPg = funcPago.get(i) ? "Pago" : "Pendente";
                    System.out.printf("[%d] Nome: %s | Cargo: %s | Salário: %s (Status: %s)\n",
                            (i + 1), funcNome.get(i), funcCargo.get(i), formatarMoeda(funcSalario.get(i)), statusPg);
                }
                System.out.println("================================================================");
            } else if (op == 2) {
                try {
                    String novoId = "";
                    while (true) {
                        System.out.print("ID Funcional com exatamente 3 caracteres/dígitos [Ex: 001] [0 ou SAIR para voltar]: "); 
                        novoId = scanner.nextLine().trim().toUpperCase();

                        if (novoId.equals("0") || novoId.equalsIgnoreCase("SAIR")) {
                            novoId = "CANCELAR";
                            break;
                        }

                        if (novoId.length() != 3) {
                            System.out.println("❌ [Erro]: O ID deve conter exatamente 3 dígitos/caracteres.");
                            System.out.println("💡 [Dica de Emergência]: Digite '0' ou 'SAIR' para cancelar e voltar ao menu anterior.\n");
                            continue;
                        }

                        if (funcId.contains(novoId)) {
                            System.out.println("❌ [Erro]: Este ID já está em uso.");
                            System.out.println("💡 [Dica de Emergência]: Digite '0' ou 'SAIR' para cancelar e voltar ao menu anterior.\n");
                            continue;
                        }
                        break;
                    }

                    if (novoId.equals("CANCELAR")) continue;

                    String novoNome = lerTextoApenasLetras(scanner, "Nome completo [Ex: Carlos Souza] [0 ou SAIR para voltar]: ");
                    if (novoNome.equals("CANCELAR")) continue;

                    String novoCpf = lerCpfValido(scanner);
                    if (novoCpf.equals("CANCELAR")) continue;

                    if (funcCpf.contains(novoCpf)) {
                        System.out.println("❌ [Erro]: Este CPF já está cadastrado.\n");
                        continue;
                    }

                    int cOpt = lerInteiroValido(scanner, "Cargo [1 - Gerente | 2 - Operador | 3 - Financeiro] [0 ou SAIR para voltar]: ");
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
                System.out.println("             ATUALIZAÇÃO DE SALÁRIO DE FUNCIONÁRIO              ");
                System.out.println("================================================================");
                System.out.println("Lista atual de funcionários cadastrados:");
                for (int i = 0; i < funcNome.size(); i++) {
                    System.out.printf("[%d] Nome: %s | Cargo: %s | Salário Atual: %s\n", (i + 1), funcNome.get(i), funcCargo.get(i), formatarMoeda(funcSalario.get(i)));
                }
                System.out.println("================================================================");
                int idx = lerInteiroValido(scanner, "Informe o número (índice) do funcionário [0 ou SAIR para voltar]: ") - 1;
                if (idx == -1000) continue; 
                if (idx >= 0 && idx < funcNome.size()) {
                    double nSal = lerDoubleValido(scanner, "Informe o novo valor do salário (R$) [SAIR para voltar]: ");
                    if (nSal == -999.0) continue;
                    funcSalario.set(idx, nSal);
                    System.out.println("✅ Sucesso! Salário atualizado.\n");
                } else {
                    System.out.println("❌ [Erro]: Funcionário não encontrado.\n");
                }
            } else if (op == 4) {
                System.out.println("\n--- LISTA DE FUNCIONÁRIOS PARA EXCLUSÃO ---");
                for (int i = 0; i < funcNome.size(); i++) {
                    System.out.printf("[%d] Nome: %s | Cargo: %s\n", (i + 1), funcNome.get(i), funcCargo.get(i));
                }
                int idx = lerInteiroValido(scanner, "\nInforme o número (índice) do funcionário que deseja excluir [0 ou SAIR para voltar]: ") - 1;
                if (idx == -1000) continue;
                if (idx >= 0 && idx < funcNome.size()) {
                    if (funcCargo.get(idx).equalsIgnoreCase("GERENTE")) {
                        long totalGerentes = funcCargo.stream().filter(c -> c.equalsIgnoreCase("GERENTE")).count();
                        if (totalGerentes <= 1) {
                            System.out.println("❌ [Erro]: Não é permitido excluir o único gerente restante no sistema!\n");
                            continue;
                        }
                    }
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
                                                   List<Double> fornLucroPretendidoPct, List<Boolean> fornPago) {
        boolean rodando = true;
        while (rodando) {
            System.out.println("\n================================================================");
            System.out.println("                    PAINEL: GESTÃO DE FORNECEDORES              ");
            System.out.println("================================================================");
            System.out.println("[1] Listar fornecedores cadastrados");
            System.out.println("[2] Cadastrar novo fornecedor");
            System.out.println("[3] Alterar preço de custo do fornecedor");
            System.out.println("[4] Excluir fornecedor");
            System.out.println("[5] Voltar ao menu anterior");
            System.out.println("================================================================");

            int op = lerInteiroValido(scanner, "Selecione uma opção [0 ou SAIR para voltar]: ");

            if (op == 5 || op == -999) {
                rodando = false;
            } else if (op == 1) {
                System.out.println("\n================================================================");
                System.out.println("                 LISTA DE FORNECEDORES ATIVOS                   ");
                System.out.println("================================================================");
                for (int i = 0; i < fornNome.size(); i++) {
                    System.out.printf("[%d] Razão Social: %s | CNPJ: %s | UF: %s | Produto: %s | Custo: %s\n",
                            (i + 1), fornNome.get(i), fornCnpj.get(i), fornUf.get(i), fornProdutoFornecido.get(i), formatarMoeda(fornPrecoPago.get(i)));
                }
                System.out.println("================================================================");
            } else if (op == 2) {
                try {
                    System.out.println("\n--- Fornecedores já cadastrados na base ---");
                    for (int i = 0; i < fornNome.size(); i++) {
                        System.out.printf("- %s (Produto: %s)\n", fornNome.get(i), fornProdutoFornecido.get(i));
                    }
                    System.out.println("--------------------------------------------");

                    String nomeForn = lerTextoApenasLetras(scanner, "Razão Social / Nome [Ex: Suplemax Ltda] [0 ou SAIR para voltar]: ");
                    if (nomeForn.equals("CANCELAR")) continue;

                    String cnpjForn = lerCnpjValido(scanner);
                    if (cnpjForn.equals("CANCELAR")) continue;

                    if (fornCnpj.contains(cnpjForn)) {
                        System.out.println("❌ [Erro]: Este CNPJ já está cadastrado.\n");
                        continue;
                    }

                    String uf = lerUfValida(scanner, "UF de origem [Ex: SP] [0 ou SAIR para voltar]: ");
                    if (uf.equals("CANCELAR")) continue;

                    System.out.print("Produto fornecido [Ex: Whey Protein] [0 ou SAIR para voltar]: "); 
                    String prod = scanner.nextLine().trim().toUpperCase();
                    if (prod.equals("0") || prod.equalsIgnoreCase("SAIR")) continue;
                    
                    double custo = lerDoubleValido(scanner, "Preço pago ao fornecedor (Custo R$) [SAIR para voltar]: ");
                    if (custo == -999.0) continue;

                    double lucro = lerDoubleValido(scanner, "Lucro pretendido para o mercado (%) [SAIR para voltar]: ");
                    if (lucro == -999.0) continue;

                    fornNome.add(nomeForn);
                    fornCnpj.add(cnpjForn);
                    fornUf.add(uf);
                    fornProdutoFornecido.add(prod);
                    fornPrecoPago.add(custo);
                    fornLucroPretendidoPct.add(lucro);
                    fornPago.add(false);

                    System.out.println("✅ Sucesso! Fornecedor cadastrado.\n");
                } catch (Exception e) {
                    System.out.println("❌ [Erro]: Falha ao registrar fornecedor.\n");
                }
            } else if (op == 3) {
                System.out.println("\n================================================================");
                System.out.println("             ATUALIZAÇÃO DE CUSTO DE FORNECEDOR                 ");
                System.out.println("================================================================");
                for (int i = 0; i < fornNome.size(); i++) {
                    System.out.printf("[%d] Razão Social: %s | Produto: %s | Custo Atual: %s\n", (i + 1), fornNome.get(i), fornProdutoFornecido.get(i), formatarMoeda(fornPrecoPago.get(i)));
                }
                System.out.println("================================================================");
                int idx = lerInteiroValido(scanner, "Informe o número (índice) do fornecedor [0 ou SAIR para voltar]: ") - 1;
                if (idx == -1000) continue;
                if (idx >= 0 && idx < fornNome.size()) {
                    double nCusto = lerDoubleValido(scanner, "Informe o novo preço de custo (R$) [SAIR para voltar]: ");
                    if (nCusto == -999.0) continue;
                    fornPrecoPago.set(idx, nCusto);
                    System.out.println("✅ Sucesso! Preço de custo atualizado.\n");
                } else {
                    System.out.println("❌ [Erro]: Fornecedor não encontrado.\n");
                }
            } else if (op == 4) {
                System.out.println("\n--- LISTA DE FORNECEDORES PARA EXCLUSÃO ---");
                for (int i = 0; i < fornNome.size(); i++) {
                    System.out.printf("[%d] %s | Produto: %s\n", (i + 1), fornNome.get(i), fornProdutoFornecido.get(i));
                }
                int idx = lerInteiroValido(scanner, "\nInforme o número (índice) do fornecedor que deseja excluir [0 ou SAIR para voltar]: ") - 1;
                if (idx == -1000) continue;
                if (idx >= 0 && idx < fornNome.size()) {
                    fornNome.remove(idx);
                    fornCnpj.remove(idx);
                    fornUf.remove(idx);
                    fornProdutoFornecido.remove(idx);
                    fornPrecoPago.remove(idx);
                    fornLucroPretendidoPct.remove(idx);
                    fornPago.remove(idx);
                    System.out.println("✅ Sucesso! Fornecedor removido.\n");
                } else {
                    System.out.println("❌ [Erro]: Fornecedor não encontrado.\n");
                }
            }
        }
    }

    /* =========================================================================
       [MÓDULO] FINANCEIRO, ESTOQUE, TRIBUTOS E HISTÓRICO DE VENDAS
       ========================================================================= */
    public static void menuFinanceiroEOperacional(Scanner scanner, List<String> prodNome, List<String> prodNcm, List<Double> prodPrecos, 
                                                   List<Integer> prodEstoque, List<Double> caixaGlobalRef, List<Double> gastosEstoqueRef,
                                                   List<Double> receitaVendasRef, List<Double> cmvRef,
                                                   List<Double> salariosPagosRef, List<Double> fornecedoresPagosRef,
                                                   List<Double> impostosPendentesRef, List<Boolean> funcPago, List<Boolean> fornPago,
                                                   List<Double> funcSalario, List<Double> fornPrecoPago, int[] anoFiscalRef,
                                                   List<String> vendaClienteCpf, List<String> vendaProdutoNome, List<Integer> vendaQtd, List<Double> vendaValorTotal,
                                                   List<String> clienteCpf, List<String> clienteNome) {
        boolean rodando = true;
        final int LIMITE_MAXIMO_PRODUTOS = 20;

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
            System.out.println("[9] Listar clientes cadastrados");
            System.out.println("[10] Exibir histórico de vendas e compras por cliente");
            System.out.println("[11] Voltar ao menu anterior");
            System.out.println("================================================================");

            int op = lerInteiroValido(scanner, "Selecione uma opção [0 ou SAIR para voltar]: ");

            if (op == 11 || op == -999) {
                rodando = false;
            } else if (op == 1) {
                System.out.println("\n================================================================");
                System.out.println("                 CATÁLOGO DE PRODUTOS E TRIBUTOS                ");
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
                    long produtosAtivos = prodNome.stream().filter(p -> p != null).count();
                    if (produtosAtivos >= LIMITE_MAXIMO_PRODUTOS) {
                        System.out.println("❌ [Erro]: Limite máximo de " + LIMITE_MAXIMO_PRODUTOS + " produtos atingido!\n");
                        continue;
                    }

                    System.out.println("\n--- Produtos já cadastrados no catálogo ---");
                    for (int i = 0; i < prodNome.size(); i++) {
                        if (prodNome.get(i) != null) {
                            System.out.printf("[%d] %s\n", (i + 1), prodNome.get(i));
                        }
                    }
                    System.out.println("---------------------------------------------");

                    int idPr = lerInteiroValido(scanner, "Informe o ID/Posição desejada para o produto [0 ou SAIR para voltar]: ") - 1;
                    if (idPr == -1000) continue;

                    if (idPr >= 0) {
                        String nProd = lerTextoApenasLetras(scanner, "Nome do produto [Ex: VITAMINA C] [0 ou SAIR para voltar]: ");
                        if (nProd.equals("CANCELAR")) continue;

                        if (prodNome.contains(nProd)) {
                            System.out.println("❌ [Erro]: Produto já cadastrado.\n");
                            continue;
                        }

                        double pVenda = lerDoubleValido(scanner, "Preço de venda (R$) [SAIR para voltar]: ");
                        if (pVenda == -999.0) continue;

                        int eIni = lerInteiroValido(scanner, "Quantidade em estoque inicial [0 ou SAIR para voltar]: ");
                        if (eIni == -999) continue;

                        // =========================================================================
                        // SELEÇÃO DINÂMICA DE NCM COM EXPLICAÇÃO E IMPOSTOS ESTIMADOS
                        // =========================================================================
                        System.out.println("\n================================================================");
                        System.out.println("             SELEÇÃO DE NCM E TRIBUTAÇÃO APLICÁVEL              ");
                        System.out.println("================================================================");
                        System.out.println("ℹ️ O NCM (Nomenclatura Comum do Mercosul) classifica o produto ");
                        System.out.println("   fiscalmente, definindo as alíquotas de ICMS, PIS e COFINS.");
                        System.out.println("----------------------------------------------------------------");

                        // Listas paralelas contendo o NCM, sua Descrição Fiscal e a Carga Tributária Estimada
                        List<String> ncmCodigos = new ArrayList<>(Arrays.asList("2106.90.30", "2106.90.90", "2202.90.00", "1704.90.20"));
                        List<String> ncmDescricoes = new ArrayList<>(Arrays.asList(
                            "Preparações alimentícias à base de aminoácidos (Suplementos/Whey)",
                            "Outras preparações alimentícias diversas (Vitaminas/Creatina)",
                            "Bebidas não alcoólicas, energéticos e isotônicos",
                            "Artigos de confeitaria e chocolates sem cacau"
                        ));
                        List<String> ncmImpostos = new ArrayList<>(Arrays.asList(
                            "ICMS 18% + PIS/COFINS Federal (Carga Média: 28.5%)",
                            "ICMS 18% + PIS/COFINS Federal (Carga Média: 29.0%)",
                            "ICMS 18% + PIS/COFINS + ST Substituição (Carga Média: 34.0%)",
                            "ICMS 18% + PIS/COFINS (Carga Média: 31.5%)"
                        ));

                        // Adiciona NCMs extras já salvos no catálogo caso existam
                        for (String ncmCadastrado : prodNcm) {
                            if (ncmCadastrado != null && !ncmCadastrado.isEmpty() && !ncmCodigos.contains(ncmCadastrado)) {
                                ncmCodigos.add(ncmCadastrado);
                                ncmDescricoes.add("NCM personalizado do catálogo interno");
                                ncmImpostos.add("Tributação Padrão do Sistema (ICMS 18%)");
                            }
                        }

                        for (int i = 0; i < ncmCodigos.size(); i++) {
                            System.out.printf("[%d] NCM: %s\n", (i + 1), ncmCodigos.get(i));
                            System.out.printf("    ↳ Descrição: %s\n", ncmDescricoes.get(i));
                            System.out.printf("    ↳ Impostos:  %s\n\n", ncmImpostos.get(i));
                        }
                        System.out.printf("[%d] Digitar um novo NCM manualmente\n", (ncmCodigos.size() + 1));
                        System.out.println("================================================================");

                        int ncmEscolha = lerInteiroValido(scanner, "Escolha a opção de NCM [0 ou SAIR para voltar]: ");
                        if (ncmEscolha == -999) continue;

                        String ncmVal = "";
                        if (ncmEscolha >= 1 && ncmEscolha <= ncmCodigos.size()) {
                            ncmVal = ncmCodigos.get(ncmEscolha - 1);
                            System.out.println("✅ NCM selecionado com sucesso: " + ncmVal + "\n");
                        } else if (ncmEscolha == ncmCodigos.size() + 1) {
                            System.out.print("Digite o novo código NCM (8 dígitos) [0 ou SAIR para voltar]: "); 
                            ncmVal = scanner.nextLine().trim();
                            if (ncmVal.equals("0") || ncmVal.equalsIgnoreCase("SAIR")) continue;
                            if (ncmVal.isEmpty()) {
                                System.out.println("❌ [Erro]: O NCM é obrigatório.\n");
                                continue;
                            }
                            System.out.println("✅ NCM manual cadastrado com sucesso!\n");
                        } else {
                            System.out.println("❌ [Erro]: Opção de NCM inválida.\n");
                            continue;
                        }

                        while (prodNome.size() <= idPr) {
                            prodNome.add(null);
                            prodNcm.add("");
                            prodPrecos.add(0.0);
                            prodEstoque.add(0);
                        }

                        prodNome.set(idPr, nProd);
                        prodNcm.set(idPr, ncmVal);
                        prodPrecos.set(idPr, pVenda);
                        prodEstoque.set(idPr, eIni);
                        System.out.println("✅ Sucesso! Produto cadastrado e vinculado à matriz fiscal.\n");
                    }
                } catch (Exception e) {
                    System.out.println("❌ [Erro]: Falha ao cadastrar.\n");
                }
            } else if (op == 3) {
                if (!prodNome.isEmpty()) {
                    System.out.println("\n================================================================");
                    System.out.println("                   TABELA DE PREÇOS ATUAIS                      ");
                    System.out.println("================================================================");
                    for (int i = 0; i < prodNome.size(); i++) {
                        if (prodNome.get(i) != null) {
                            System.out.println("ID: " + (i + 1) + " | " + prodNome.get(i) + " | Preço: " + formatarMoeda(prodPrecos.get(i)));
                        }
                    }
                    System.out.println("================================================================");
                    int idPr = lerInteiroValido(scanner, "Informe o ID do produto para alteração de preço [0 ou SAIR para voltar]: ") - 1;
                    if (idPr == -1000) continue;
                    if (idPr >= 0 && idPr < prodNome.size() && prodNome.get(idPr) != null) {
                        double novoPreco = lerDoubleValido(scanner, "Novo preço de venda (R$) [SAIR para voltar]: ");
                        if (novoPreco == -999.0) continue;
                        prodPrecos.set(idPr, novoPreco);
                        System.out.println("✅ Sucesso! Preço atualizado.\n");
                    } else {
                        System.out.println("❌ [Erro]: Produto não encontrado.\n");
                    }
                } else {
                    System.out.println("⚠️ [Aviso]: Nenhum produto cadastrado.\n");
                }
            } else if (op == 4) {
                System.out.println("\n--- MOVIMENTAÇÃO DE ESTOQUE E COMPRAS ---");
                for (int i = 0; i < prodNome.size(); i++) {
                    if (prodNome.get(i) != null) {
                        System.out.printf("[%d] %s (Estoque atual: %d)\n", (i + 1), prodNome.get(i), prodEstoque.get(i));
                    }
                }
                int idx = lerInteiroValido(scanner, "Selecione o produto [0 ou SAIR para voltar]: ") - 1;
                if (idx == -1000) continue;
                if (idx >= 0 && idx < prodNome.size() && prodNome.get(idx) != null) {
                    int qtd = lerInteiroValido(scanner, "Quantidade a adicionar ao estoque [0 ou SAIR para voltar]: ");
                    if (qtd == -999) continue;
                    prodEstoque.set(idx, prodEstoque.get(idx) + qtd);
                    double custoUnitarioEstimado = 35.00;
                    double custoTotalCompra = qtd * custoUnitarioEstimado;
                    gastosEstoqueRef.set(0, gastosEstoqueRef.get(0) + custoTotalCompra);
                    caixaGlobalRef.set(0, caixaGlobalRef.get(0) - custoTotalCompra);
                    System.out.println("✅ Estoque atualizado! Custo debitado do caixa: " + formatarMoeda(custoTotalCompra) + "\n");
                } else {
                    System.out.println("❌ [Erro]: Produto inválido.\n");
                }
            } else if (op == 5) {
                int anoCfg = lerInteiroValido(scanner, "Digite o ano fiscal (2026 ou 2027) [0 ou SAIR para voltar]: ");
                if (anoCfg == 2026 || anoCfg == 2027) {
                    anoFiscalRef[0] = anoCfg;
                    System.out.println("✅ Ano fiscal configurado para: " + anoFiscalRef[0] + "\n");
                } else {
                    System.out.println("❌ [Erro]: Ano inválido. Utilize 2026 ou 2027.\n");
                }
            } else if (op == 6) {
                double lucroBrutoReal = receitaVendasRef.get(0) - cmvRef.get(0);
                System.out.println("\n================================================================");
                System.out.println("            BALANÇO FINANCEIRO E DEMONSTRATIVO DRE              ");
                System.out.println("================================================================");
                System.out.printf(" Saldo em Caixa:             %s\n", formatarMoeda(caixaGlobalRef.get(0)));
                System.out.printf(" (+) Receita de Vendas:      %s\n", formatarMoeda(receitaVendasRef.get(0)));
                System.out.printf(" (-) CMV (Custo Merc. Vend.):%s\n", formatarMoeda(cmvRef.get(0)));
                System.out.printf(" (=) Lucro Bruto Operacional:%s\n", formatarMoeda(lucroBrutoReal));
                System.out.printf(" (-) Total Gastos Estoque:   %s\n", formatarMoeda(gastosEstoqueRef.get(0)));
                System.out.printf(" (-) Salários Pagos:         %s\n", formatarMoeda(salariosPagosRef.get(0)));
                System.out.printf(" (-) Fornecedores Pagos:     %s\n", formatarMoeda(fornecedoresPagosRef.get(0)));
                System.out.printf(" (-) Impostos Pendentes:     %s\n", formatarMoeda(impostosPendentesRef.get(0)));
                System.out.println("================================================================");
            } else if (op == 7) {
                System.out.println("\n================================================================");
                System.out.println("          CENTRAL DE PAGAMENTOS E OBRIGAÇÕES FISCAIS            ");
                System.out.println("================================================================");
                System.out.println("[1] Pagar Salários de Funcionários");
                System.out.println("[2] Pagar Fornecedores");
                System.out.println("[3] Pagar Impostos Pendentes (ICMS/PIS/COFINS)");
                int subOp = lerInteiroValido(scanner, "Escolha uma opção [0 ou SAIR para voltar]: ");

                if (subOp == 1) {
                    double totalSal = 0.0;
                    for (int i = 0; i < funcSalario.size(); i++) {
                        if (!funcPago.get(i)) {
                            totalSal += funcSalario.get(i);
                            funcPago.set(i, true);
                        }
                    }
                    salariosPagosRef.set(0, salariosPagosRef.get(0) + totalSal);
                    caixaGlobalRef.set(0, caixaGlobalRef.get(0) - totalSal);
                    System.out.println("✅ Salários quitados! Total pago: " + formatarMoeda(totalSal) + "\n");
                } else if (subOp == 2) {
                    double totalForn = 0.0;
                    for (int i = 0; i < fornPrecoPago.size(); i++) {
                        if (!fornPago.get(i)) {
                            totalForn += fornPrecoPago.get(i);
                            fornPago.set(i, true);
                        }
                    }
                    fornecedoresPagosRef.set(0, fornecedoresPagosRef.get(0) + totalForn);
                    caixaGlobalRef.set(0, caixaGlobalRef.get(0) - totalForn);
                    System.out.println("✅ Fornecedores quitados! Total pago: " + formatarMoeda(totalForn) + "\n");
                } else if (subOp == 3) {
                    if (impostosPendentesRef.get(0) > 0) {
                        caixaGlobalRef.set(0, caixaGlobalRef.get(0) - impostosPendentesRef.get(0));
                        System.out.println("✅ Impostos quitados! Total pago: " + formatarMoeda(impostosPendentesRef.get(0)) + "\n");
                        impostosPendentesRef.set(0, 0.0);
                    } else {
                        System.out.println(">>> Não há impostos pendentes no momento. <<<\n");
                    }
                }
            } else if (op == 8) {
                System.out.println("\n--- EXCLUIR PRODUTO DO CATÁLOGO ---");
                for (int i = 0; i < prodNome.size(); i++) {
                    if (prodNome.get(i) != null) {
                        System.out.printf("[%d] %s\n", (i + 1), prodNome.get(i));
                    }
                }
                int idx = lerInteiroValido(scanner, "Informe o número do produto que deseja remover [0 ou SAIR para voltar]: ") - 1;
                if (idx == -1000) continue;
                if (idx >= 0 && idx < prodNome.size() && prodNome.get(idx) != null) {
                    prodNome.remove(idx);
                    prodNcm.remove(idx);
                    prodPrecos.remove(idx);
                    prodEstoque.remove(idx);
                    System.out.println("✅ Sucesso! Produto removido do catálogo.\n");
                } else {
                    System.out.println("❌ [Erro]: Produto não encontrado.\n");
                }
            } else if (op == 9) {
                System.out.println("\n================================================================");
                System.out.println("                  LISTA DE CLIENTES CADASTRADOS                 ");
                System.out.println("================================================================");
                if (clienteCpf.isEmpty()) {
                    System.out.println("⚠️ Nenhum cliente cadastrado no sistema ainda.");
                } else {
                    for (int i = 0; i < clienteCpf.size(); i++) {
                        System.out.printf("[%d] Nome: %s | CPF: %s\n", (i + 1), clienteNome.get(i), clienteCpf.get(i));
                    }
                }
                System.out.println("================================================================");
            } else if (op == 10) {
                System.out.println("\n================================================================");
                System.out.println("            HISTÓRICO FINANCEIRO DE VENDAS POR CLIENTE          ");
                System.out.println("================================================================");
                if (vendaClienteCpf.isEmpty()) {
                    System.out.println("⚠️ Nenhuma venda registrada no histórico financeiro.");
                } else {
                    for (int i = 0; i < vendaClienteCpf.size(); i++) {
                        String cpfC = vendaClienteCpf.get(i);
                        String nomeC = "Desconhecido";
                        int idxCli = clienteCpf.indexOf(cpfC);
                        if (idxCli != -1) {
                            nomeC = clienteNome.get(idxCli);
                        }
                        System.out.printf("[%d] Cliente: %s (CPF: %s) | Produto: %s | Qtd: %d | Total: %s\n",
                                (i + 1), nomeC, cpfC, vendaProdutoNome.get(i), vendaQtd.get(i), formatarMoeda(vendaValorTotal.get(i)));
                    }
                }
                System.out.println("================================================================");
            }
        }
    }

    /* =========================================================================
       [MÓDULO DE LOJA / CLIENTE] CHECKOUT, CLIENTE PERMANENTE E HISTÓRICO
       ========================================================================= */
    public static void fluxoModuloCliente(Scanner scanner, List<String> prodNome, List<Double> prodPrecos, 
                                          List<Integer> prodEstoque, List<Double> caixaGlobalRef, 
                                          List<Double> receitaVendasRef, List<Double> cmvRef,
                                          List<String> clienteCpf, List<String> clienteNome,
                                          List<String> vendaClienteCpf, List<String> vendaProdutoNome, 
                                          List<Integer> vendaQtd, List<Double> vendaValorTotal) {
        boolean comprando = true;
        double valorTotalCarrinho = 0.0;
        List<String> itensCarrinho = new ArrayList<>();
        List<Integer> qtdCarrinho = new ArrayList<>();
        List<Double> subtotalCarrinho = new ArrayList<>();

        while (comprando) {
            System.out.println("\n================================================================");
            System.out.println("              L'ESTOQUE - CATÁLOGO DE PRODUTOS                  ");
            System.out.println("================================================================");
            for (int i = 0; i < prodNome.size(); i++) {
                if (prodNome.get(i) != null) {
                    System.out.printf("[%d] %s | Preço: %s | Estoque Disp: %d\n",
                            (i + 1), prodNome.get(i), formatarMoeda(prodPrecos.get(i)), prodEstoque.get(i));
                }
            }
            System.out.println("----------------------------------------------------------------");
            System.out.println("[99] Finalizar Compra / Ir para Pagamento");
            System.out.println("[0] Voltar ao Menu Principal");
            System.out.println("================================================================");

            int escolha = lerInteiroValido(scanner, "Escolha o número do produto para comprar (ou 99 para finalizar) [0 ou SAIR para voltar]: ");

            if (escolha == 0 || escolha == -999) {
                return;
            } else if (escolha == 99) {
                if (itensCarrinho.isEmpty()) {
                    System.out.println("⚠️ [Aviso]: Seu carrinho está vazio!\n");
                    continue;
                }
                
                System.out.println("\n================================================================");
                System.out.println("                 FINALIZAÇÃO DA COMPRA                          ");
                System.out.println("================================================================");
                System.out.println("Resumo do Carrinho:");
                for (int i = 0; i < itensCarrinho.size(); i++) {
                    System.out.printf("- %s (Qtd: %d) -> Subtotal: %s\n", itensCarrinho.get(i), qtdCarrinho.get(i), formatarMoeda(subtotalCarrinho.get(i)));
                }
                System.out.printf("VALOR TOTAL: %s\n", formatarMoeda(valorTotalCarrinho));
                System.out.println("----------------------------------------------------------------");
                
                System.out.print("Deseja confirmar a compra e proceder para o pagamento? (S/N) [SAIR para voltar]: ");
                String conf = scanner.nextLine().trim();
                if (conf.equalsIgnoreCase("SAIR") || !conf.equalsIgnoreCase("S")) {
                    System.out.println("[INFO] Operação de compra cancelada.\n");
                    continue;
                }

                System.out.println("\n--- IDENTIFICAÇÃO DO CLIENTE ---");
                if (!clienteNome.isEmpty()) {
                    System.out.println("Clientes já cadastrados no sistema:");
                    for (int i = 0; i < clienteNome.size(); i++) {
                        System.out.printf("- Nome: %s\n", clienteNome.get(i));
                    }
                    System.out.println("----------------------------------------");
                }

                String cpfCliente = lerCpfValido(scanner);
                if (cpfCliente.equals("CANCELAR")) continue;

                int indexCliente = clienteCpf.indexOf(cpfCliente);
                String nomeClienteFinal = "";

                if (indexCliente != -1) {
                    nomeClienteFinal = clienteNome.get(indexCliente);
                    System.out.println("✅ Cliente reconhecido: " + nomeClienteFinal + " (Cadastrado no sistema).");
                } else {
                    System.out.println("ℹ️ CPF não encontrado na base de clientes. Realizando novo cadastro...");
                    nomeClienteFinal = lerTextoApenasLetras(scanner, "Digite o nome completo do novo cliente [0 ou SAIR para voltar]: ");
                    if (nomeClienteFinal.equals("CANCELAR")) continue;

                    clienteCpf.add(cpfCliente);
                    clienteNome.add(nomeClienteFinal);
                    System.out.println("✅ Novo cliente cadastrado com sucesso!");
                }

                System.out.println("\n--- ESCOLHA A FORMA DE PAGAMENTO ---");
                System.out.println("[1] Dinheiro / PIX (À vista)");
                System.out.println("[2] Cartão de Crédito / Débito");
                int formaPagto = lerInteiroValido(scanner, "Selecione a opção de pagamento [0 ou SAIR para voltar]: ");

                if (formaPagto == 1 || formaPagto == 2) {
                    caixaGlobalRef.set(0, caixaGlobalRef.get(0) + valorTotalCarrinho);
                    receitaVendasRef.set(0, receitaVendasRef.get(0) + valorTotalCarrinho);
                    
                    double custoEstimadoItem = 30.00;
                    double totalCmvVenda = 0;

                    for (int i = 0; i < itensCarrinho.size(); i++) {
                        int q = qtdCarrinho.get(i);
                        double sub = subtotalCarrinho.get(i);
                        totalCmvVenda += (q * custoEstimadoItem);

                        vendaClienteCpf.add(cpfCliente);
                        vendaProdutoNome.add(itensCarrinho.get(i));
                        vendaQtd.add(q);
                        vendaValorTotal.add(sub);
                    }
                    cmvRef.set(0, cmvRef.get(0) + totalCmvVenda);

                    System.out.println("\n================================================================");
                    System.out.println("✅ PAGAMENTO APROVADO! COMPRA REALIZADA COM SUCESSO.");
                    System.out.printf("Cliente: %s | Total Pago: %s | Forma: %s\n", nomeClienteFinal, formatarMoeda(valorTotalCarrinho), (formaPagto == 1 ? "PIX/Dinheiro" : "Cartão"));
                    System.out.println("Obrigado por comprar no Protei Creat / L'Estoque!");
                    System.out.println("================================================================");
                    return;
                } else {
                    System.out.println("❌ [Erro]: Forma de pagamento inválida. Compra não concluída.\n");
                }

            } else {
                int idx = escolha - 1;
                if (idx >= 0 && idx < prodNome.size() && prodNome.get(idx) != null) {
                    int estoqueAtual = prodEstoque.get(idx);
                    if (estoqueAtual <= 0) {
                        System.out.println("❌ [Erro]: Produto esgotado em estoque!\n");
                        continue;
                    }

                    int qtdDesejada = lerInteiroValido(scanner, "Digite a quantidade desejada [0 ou SAIR para voltar]: ");
                    
                    if (qtdDesejada == -999) continue;

                    if (qtdDesejada == 0) {
                        System.out.println("⚠️ [Aviso]: A quantidade não pode ser zero. Produto não adicionado ao carrinho.\n");
                        continue;
                    }

                    if (qtdDesejada > estoqueAtual) {
                        System.out.println("❌ [Erro]: Quantidade solicitada maior do que o estoque disponível (" + estoqueAtual + ").\n");
                        continue;
                    }

                    prodEstoque.set(idx, estoqueAtual - qtdDesejada);
                    double subtotal = qtdDesejada * prodPrecos.get(idx);
                    
                    itensCarrinho.add(prodNome.get(idx));
                    qtdCarrinho.add(qtdDesejada);
                    subtotalCarrinho.add(subtotal);
                    valorTotalCarrinho += subtotal;

                    System.out.println("✅ " + qtdDesejada + "x " + prodNome.get(idx) + " adicionado(s) ao carrinho!\n");
                } else {
                    System.out.println("❌ [Erro]: Produto inválido.\n");
                }
            }
        }
    }

    /* =========================================================================
       [MÓDULO PRINCIPAL] MENU PRINCIPAL E AUTENTICAÇÃO ADMINISTRATIVA
       ========================================================================= */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        List<String> funcId = new ArrayList<>(Arrays.asList("001", "002", "003"));
        List<String> funcNome = new ArrayList<>(Arrays.asList("ANA SILVA", "BRUNO LIMA", "CAMILA SOUZA"));
        List<String> funcCpf = new ArrayList<>(Arrays.asList("52998224725", "12345678909", "98765432100"));
        List<String> funcCargo = new ArrayList<>(Arrays.asList("GERENTE", "OPERADOR", "FINANCEIRO"));
        List<String> funcSenha = new ArrayList<>(Arrays.asList("admin123", "op456", "fin789"));
        List<Double> funcSalario = new ArrayList<>(Arrays.asList(5500.00, 2200.00, 4800.00));
        List<Boolean> funcPago = new ArrayList<>(Arrays.asList(false, false, false));

        List<String> fornNome = new ArrayList<>(Arrays.asList("SUPLEMAX LTDA", "VITAFIT DISTRIBUIDORA"));
        List<String> fornCnpj = new ArrayList<>(Arrays.asList("12345678000190", "98765432000110"));
        List<String> fornUf = new ArrayList<>(Arrays.asList("SP", "RJ"));
        List<String> fornProdutoFornecido = new ArrayList<>(Arrays.asList("WHEY PROTEIN", "CREATINA"));
        List<Double> fornPrecoPago = new ArrayList<>(Arrays.asList(4500.00, 3200.00));
        List<Double> fornLucroPretendidoPct = new ArrayList<>(Arrays.asList(40.0, 35.0));
        List<Boolean> fornPago = new ArrayList<>(Arrays.asList(false, false));

        List<String> clienteCpf = new ArrayList<>(Arrays.asList("11144477735"));
        List<String> clienteNome = new ArrayList<>(Arrays.asList("JOAO PEREIRA"));

        List<String> vendaClienteCpf = new ArrayList<>();
        List<String> vendaProdutoNome = new ArrayList<>();
        List<Integer> vendaQtd = new ArrayList<>();
        List<Double> vendaValorTotal = new ArrayList<>();

        List<String> prodNome = new ArrayList<>(Arrays.asList("WHEY PROTEIN 1KG", "CREATINA 300G"));
        List<String> prodNcm = new ArrayList<>(Arrays.asList("2106.90.30", "2106.90.90"));
        List<Double> prodPrecos = new ArrayList<>(Arrays.asList(120.00, 80.00));
        List<Integer> prodEstoque = new ArrayList<>(Arrays.asList(50, 40));

        List<Double> caixaGlobal = new ArrayList<>(Arrays.asList(100000.00));
        List<Double> gastosEstoque = new ArrayList<>(Arrays.asList(15000.00));
        List<Double> receitaVendas = new ArrayList<>(Arrays.asList(0.0));
        List<Double> cmv = new ArrayList<>(Arrays.asList(0.0));
        List<Double> salariosPagos = new ArrayList<>(Arrays.asList(0.0));
        List<Double> fornecedoresPagos = new ArrayList<>(Arrays.asList(0.0));
        List<Double> impostosPendentes = new ArrayList<>(Arrays.asList(1250.00));
        int[] anoFiscal = {2026};

        exibirCabecalho();

        boolean sistemaAtivo = true;
        while (sistemaAtivo) {
            System.out.println("\n================================================================");
            System.out.println("                 MENU PRINCIPAL DE ACESSO                       ");
            System.out.println("================================================================");
            System.out.println("[1] Acessar como Cliente (Ver Produtos e Comprar Direto)");
            System.out.println("[2] Acessar Painel Administrativo / Funcionários (Login)");
            System.out.println("[3] Sair do Sistema");
            System.out.println("================================================================");

            int opcaoInicial = lerInteiroValido(scanner, "Selecione uma opção [0 ou SAIR para voltar]: ");

            if (opcaoInicial == 1) {
                fluxoModuloCliente(scanner, prodNome, prodPrecos, prodEstoque, caixaGlobal, receitaVendas, cmv,
                        clienteCpf, clienteNome, vendaClienteCpf, vendaProdutoNome, vendaQtd, vendaValorTotal);
            } else if (opcaoInicial == 2) {
                boolean autenticado = false;
                String cargoLogado = "";

                System.out.println("\n=== AUTENTICAÇÃO ADMINISTRATIVA ===");
                System.out.println("Funcionários cadastrados no sistema (IDs ocultos por segurança):");
                for (int i = 0; i < funcNome.size(); i++) {
                    System.out.printf("- Nome: %s | Cargo: %s\n", funcNome.get(i), funcCargo.get(i));
                }
                System.out.println("----------------------------------------------------------------");

                String idInput = "";
                while (true) {
                    System.out.print("ID Funcional (Ex: 001) [0 ou SAIR para voltar]: ");
                    idInput = scanner.nextLine().trim().toUpperCase();
                    
                    if (idInput.equals("0") || idInput.equalsIgnoreCase("SAIR")) {
                        idInput = "CANCELAR";
                        break;
                    }

                    if (idInput.length() != 3) {
                        System.out.println("❌ [Erro]: O ID deve conter exatamente 3 dígitos.\n");
                        continue;
                    }

                    if (!funcId.contains(idInput)) {
                        System.out.println("❌ [Erro]: Este ID não está cadastrado no sistema.\n");
                        continue;
                    }
                    break;
                }

                if (idInput.equals("CANCELAR")) {
                    System.out.println(">>> Retornando ao menu principal. <<<\n");
                    continue;
                }

                int index = funcId.indexOf(idInput);

                for (int tentativa = 1; tentativa <= 3; tentativa++) {
                    System.out.print("Senha (Tentativa " + tentativa + " de 3): ");
                    String senhaInput = scanner.nextLine().trim();

                    if (funcSenha.get(index).equals(senhaInput)) {
                        autenticado = true;
                        cargoLogado = funcCargo.get(index);
                        System.out.println("\n✅ Login bem-sucedido! Bem-vindo(a), " + funcNome.get(index) + " (" + cargoLogado + ").\n");
                        break;
                    } else {
                        System.out.println("❌ Senha incorreta.\n");
                    }
                }

                if (!autenticado) {
                    System.out.println(">>> Número máximo de tentativas de senha atingido. Retornando ao menu principal. <<<\n");
                    continue;
                }

                boolean adminAtivo = true;
                while (adminAtivo) {
                    System.out.println("\n================================================================");
                    System.out.println("                 PAINEL ADMINISTRATIVO (" + cargoLogado + ")      ");
                    System.out.println("================================================================");
                    if (cargoLogado.equalsIgnoreCase("GERENTE")) {
                        System.out.println("[1] Gerenciar Funcionários");
                        System.out.println("[2] Gerenciar Fornecedores");
                        System.out.println("[3] Painel Financeiro, Estoque e Tributos");
                    } else if (cargoLogado.equalsIgnoreCase("FINANCEIRO")) {
                        System.out.println("[2] Gerenciar Fornecedores");
                        System.out.println("[3] Painel Financeiro, Estoque e Tributos");
                    } else {
                        System.out.println("(Operador sem permissões administrativas adicionais)");
                    }
                    System.out.println("[4] Sair do Painel Administrativo");
                    System.out.println("================================================================");

                    int opAdmin = lerInteiroValido(scanner, "Selecione uma opção [0 ou SAIR para voltar]: ");

                    if (opAdmin == 1 && cargoLogado.equalsIgnoreCase("GERENTE")) {
                        menuGerenciarFuncionarios(scanner, funcId, funcNome, funcCpf, funcCargo, funcSenha, funcSalario, funcPago);
                    } else if (opAdmin == 2 && !cargoLogado.equalsIgnoreCase("OPERADOR")) {
                        menuGerenciarFornecedores(scanner, fornNome, fornCnpj, fornUf, fornProdutoFornecido, fornPrecoPago, fornLucroPretendidoPct, fornPago);
                    } else if (opAdmin == 3 && !cargoLogado.equalsIgnoreCase("OPERADOR")) {
                        menuFinanceiroEOperacional(scanner, prodNome, prodNcm, prodPrecos, prodEstoque,
                                caixaGlobal, gastosEstoque, receitaVendas, cmv, salariosPagos, fornecedoresPagos, impostosPendentes,
                                funcPago, fornPago, funcSalario, fornPrecoPago, anoFiscal,
                                vendaClienteCpf, vendaProdutoNome, vendaQtd, vendaValorTotal, clienteCpf, clienteNome);
                    } else if (opAdmin == 4 || opAdmin == -999) {
                        adminAtivo = false;
                    } else {
                        System.out.println("❌ Opção inválida ou não autorizada para o seu cargo!\n");
                    }
                }

            } else if (opcaoInicial == 3 || opcaoInicial == -999) {
                sistemaAtivo = false;
                System.out.println("\n[INFO] Encerrando o sistema Protei Creat. Obrigado por utilizar!\n");
            } else {
                System.out.println("❌ Opção inválida! Tente novamente.\n");
            }
        }
        scanner.close();
    }
}