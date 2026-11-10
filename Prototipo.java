
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.time.DateTimeException;

public class Prototipo {

    /* =========================================================================
       [MÓDULO 1] TRIBUTÁRIO E DE MATRIZ FISCAL (RECEITA FEDERAL / 2026-2027)
       ========================================================================= */
    public static double aliquotaIcmsInterno2026 = 0.18;
    public static double aliquotaIcmsInterno2027 = 0.19;
    public static double aliquotaPisPadrao = 0.0165;
    public static double aliquotaCofinsPadrao = 0.0760;

    // Estado financeiro global (garante persistência das alterações)
    public static double saldoCaixaLoja = 100000.00;
    public static double totalGastosEstoque = 15000.00;
    public static double totalReceitaVendas = 0.0;
    public static double totalSalariosPagos = 0.0;
    public static double totalFornecedoresPagos = 0.0;
    public static double impostosPendentesTotal = 1250.00;
    public static String ufOrigem = "SP";
    public static int anoExercicioPadrao = 2026;

    public static double buscarAliquotaIcms(String ncm, String ufOrigem, String ufDestino, int anoExercicio) {
        if (ncm == null || ufOrigem == null || ufDestino == null) {
            return 0.12;
        }
        if (anoExercicio != 2026 && anoExercicio != 2027) {
            anoExercicio = 2026;
        }
        double aliquotaBase = (anoExercicio == 2027) ? aliquotaIcmsInterno2027 : aliquotaIcmsInterno2026;
        return (ufOrigem.equalsIgnoreCase("SP") && ufDestino.equalsIgnoreCase("SP")) ? aliquotaBase : 0.12;
    }

    public static double buscarAliquotaPis(String ncm) {
        if (ncm == null) return aliquotaPisPadrao;
        if (ncm.equals("2202.99.00")) return 0.00;
        return aliquotaPisPadrao;
    }

    public static double buscarAliquotaCofins(String ncm) {
        if (ncm == null) return aliquotaCofinsPadrao;
        if (ncm.equals("2202.99.00")) return 0.00;
        return aliquotaCofinsPadrao;
    }

    public static double calcularImpostoItem(double valorBase, double aliquota) {
        return valorBase * aliquota;
    }

    /* =========================================================================
       [MÓDULO 2] SEGURANÇA E UTILITÁRIOS
       ========================================================================= */
    public static String geradorSenhaAleatoriaSistema() {
        String caracteres = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$";
        SecureRandom random = new SecureRandom();
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
       [MÓDULO 3] VALIDAÇÃO ROBUSTA DE ENTRADAS
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
                    System.out.println(">>> ERRO: O campo nao pode ficar vazio!");
                    System.out.println(">>> O que e permitido: Apenas letras e espacos.");
                    System.out.println(">>> Exemplo correto: MARIO SILVA. Digite '0' ou 'SAIR' para cancelar. <<<\n");
                } else if (!entrada.matches("^[a-zA-ZáàâãéèêíïóôõúüçÁÀÂÃÉÈÊÍÏÓÔÕÚÜÇ\\s]+$")) {
                    System.out.println(">>> ERRO: Este campo aceita apenas letras e espacos.");
                    System.out.println(">>> Exemplo correto: MARIO SILVA. Digite '0' ou 'SAIR' para cancelar. <<<\n");
                } else {
                    break;
                }
            } catch (Exception e) {
                System.out.println(">>> ERRO: Falha na leitura do texto. Tente novamente. <<<\n");
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
                    System.out.println(">>> ERRO: UF invalida! Digite uma sigla de estado valida.");
                    System.out.println(">>> Exemplo correto: SP. Digite '0' para voltar. <<<\n");
                }
            } catch (Exception e) {
                System.out.println(">>> ERRO: Falha na validacao da UF. <<<\n");
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
                String apenasNumeros = entrada.replaceAll("[^0-9]", "");
                if (apenasNumeros.isEmpty()) {
                    System.out.println(">>> ERRO: Digite apenas numeros inteiros validos.");
                    System.out.println(">>> Exemplo correto: 2. Digite '0' para voltar. <<<\n");
                    continue;
                }
                valor = Integer.parseInt(apenasNumeros);
                break;
            } catch (Exception e) {
                System.out.println(">>> ERRO: Entrada invalida. Digite apenas numeros.");
                System.out.println(">>> Exemplo correto: 2. Digite '0' para voltar. <<<\n");
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
                    System.out.println(">>> ERRO: O valor nao pode ser negativo.");
                    System.out.println(">>> Exemplo correto: 49.90. Digite '0' para voltar. <<<\n");
                    continue;
                }
                break;
            } catch (Exception e) {
                System.out.println(">>> ERRO: Formato numerico invalido.");
                System.out.println(">>> Exemplo correto: 49.90. Digite '0' para voltar. <<<\n");
            }
        }
        return valor;
    }

    public static String lerCpfValido(Scanner scanner) {
        String cpf = "";
        while (true) {
            try {
                System.out.print("Digite o CPF (11 numeros) [Exemplo: 52998224725] (0 ou SAIR para voltar): ");
                String entrada = scanner.nextLine().trim();

                if (entrada.equalsIgnoreCase("0") || entrada.equalsIgnoreCase("SAIR")) {
                    return "CANCELAR";
                }

                cpf = entrada.replaceAll("[^0-9]", "");

                if (cpf.length() != 11 || cpf.matches("(\\d)\\1{10}")) {
                    System.out.println(">>> ERRO: CPF invalido. Deve conter exatamente 11 digitos e nao pode ser sequencia repetida.");
                    System.out.println(">>> Exemplo correto: 52998224725. Digite '0' ou 'SAIR' para abortar. <<<\n");
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

                if (dv1 == Character.getNumericValue(cpf.charAt(9)) &&
                    dv2 == Character.getNumericValue(cpf.charAt(10))) {
                    break;
                } else {
                    System.out.println(">>> ERRO: Digitos verificadores do CPF incorretos.");
                    System.out.println(">>> Exemplo correto: 52998224725. Digite '0' ou 'SAIR' para abortar. <<<\n");
                }
            } catch (Exception e) {
                System.out.println(">>> ERRO: Falha ao validar o CPF. <<<\n");
            }
        }
        return cpf;
    }

    public static String lerCnpjValido(Scanner scanner) {
        String cnpj = "";
        while (true) {
            try {
                System.out.print("Digite o CNPJ (14 numeros) [Exemplo: 12345678000190] (0 ou SAIR para voltar): ");
                String entrada = scanner.nextLine().trim();

                if (entrada.equalsIgnoreCase("0") || entrada.equalsIgnoreCase("SAIR")) {
                    return "CANCELAR";
                }

                cnpj = entrada.replaceAll("[^0-9]", "");

                if (cnpj.length() != 14 || cnpj.matches("(\\d)\\1{13}")) {
                    System.out.println(">>> ERRO: CNPJ invalido. Deve conter 14 digitos.");
                    System.out.println(">>> Digite '0' ou 'SAIR' para abortar. <<<\n");
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

                if (dv1 == Character.getNumericValue(cnpj.charAt(12)) &&
                    dv2 == Character.getNumericValue(cnpj.charAt(13))) {
                    break;
                } else {
                    System.out.println(">>> ERRO: Digitos verificadores do CNPJ incorretos.");
                    System.out.println(">>> Digite '0' ou 'SAIR' para abortar. <<<\n");
                }
            } catch (Exception e) {
                System.out.println(">>> ERRO: Falha ao validar o CNPJ. <<<\n");
            }
        }
        return cnpj;
    }

    public static String lerCepValido(Scanner scanner) {
        String cep = "";
        while (true) {
            try {
                System.out.print("Digite o CEP (8 numeros) [Exemplo: 04753060] (0 ou SAIR para voltar): ");
                String entrada = scanner.nextLine().trim();

                if (entrada.equalsIgnoreCase("0") || entrada.equalsIgnoreCase("SAIR")) {
                    return "CANCELAR";
                }

                cep = entrada.replaceAll("[^0-9]", "");

                if (cep.length() != 8) {
                    System.out.println(">>> ERRO: CEP invalido. Deve conter 8 digitos.");
                    System.out.println(">>> Exemplo correto: 04753060. Digite '0' ou 'SAIR' para abortar. <<<\n");
                } else {
                    break;
                }
            } catch (Exception e) {
                System.out.println(">>> ERRO: Falha na leitura do CEP. <<<\n");
            }
        }
        return cep;
    }

    public static double obterSalarioPadraoCargo(String cargo) {
        if (cargo.equalsIgnoreCase("GERENTE")) return 5500.00;
        if (cargo.equalsIgnoreCase("FINANCEIRO")) return 4800.00;
        return 2200.00;
    }

    public static String verificarAlertaMediaSalarial(String cargo, double salario) {
        double min = 0.0, max = 0.0;
        if (cargo.equalsIgnoreCase("GERENTE")) {
            min = 4500.00; max = 7000.00;
        } else if (cargo.equalsIgnoreCase("FINANCEIRO")) {
            min = 3800.00; max = 5800.00;
        } else {
            min = 1800.00; max = 3000.00;
        }
        if (salario < min) return " [ALERTA: ABAIXO DA MEDIA]";
        if (salario > max) return " [ALERTA: ACIMA DA MEDIA]";
        return " [DENTRO DA MEDIA]";
    }

    public static void exibirCabecalho() {
        System.out.println("\n================================================================");
        System.out.println("            Bem vindo ao Protei Creat                           ");
        System.out.println("          SISTEMA DE GESTAO TRIBUTARIA E ESTOQUE - L'ESTOQUE    ");
        System.out.println("================================================================");
    }

    /* =========================================================================
       [MÓDULO] GESTÃO DE FUNCIONÁRIOS
       ========================================================================= */
    public static void menuGerenciarFuncionarios(Scanner scanner, List<String> funcId, List<String> funcNome,
                                                 List<String> funcCpf, List<String> funcCargo, List<String> funcSenha,
                                                 List<Double> funcSalario, List<Boolean> funcPago) {
        boolean rodando = true;
        while (rodando) {
            System.out.println("\n================================================================");
            System.out.println("                   PAINEL: GESTAO DE FUNCIONARIOS               ");
            System.out.println("================================================================");
            System.out.println("[1] Listar funcionarios cadastrados");
            System.out.println("[2] Cadastrar novo funcionario");
            System.out.println("[3] Alterar salario de um funcionario");
            System.out.println("[4] Excluir funcionario");
            System.out.println("[5] Voltar ao menu administrativo anterior");
            System.out.println("================================================================");

            int op = lerInteiroValido(scanner, "Selecione uma opcao (0 para voltar): ");

            if (op == 5 || op == -999) {
                rodando = false;
            } else if (op == 1) {
                System.out.println("\n================================================================");
                System.out.println("                  LISTA DE FUNCIONARIOS ATIVOS                  ");
                System.out.println("================================================================");
                for (int i = 0; i < funcNome.size(); i++) {
                    String statusPg = funcPago.get(i) ? "Pago" : "Pendente";
                    String alerta = verificarAlertaMediaSalarial(funcCargo.get(i), funcSalario.get(i));
                    System.out.printf("[%d] ID: %s | Nome: %s | Cargo: %s | Salario: %s (Status: %s)%s\n",
                            (i + 1), funcId.get(i), funcNome.get(i), funcCargo.get(i),
                            formatarMoeda(funcSalario.get(i)), statusPg, alerta);
                }
                System.out.println("================================================================");
            } else if (op == 2) {
                try {
                    System.out.print("ID Funcional Exclusivo [Ex: F04] (0 para voltar): ");
                    String novoId = scanner.nextLine().trim().toUpperCase();
                    if (novoId.equals("0") || novoId.equalsIgnoreCase("SAIR")) continue;

                    if (funcId.contains(novoId)) {
                        System.out.println(">>> ERRO: Este ID ja esta em uso. <<<\n");
                        continue;
                    }

                    String novoNome = lerTextoApenasLetras(scanner, "Nome completo [Ex: Carlos Souza] (0 para voltar): ");
                    if (novoNome.equals("CANCELAR")) continue;

                    String novoCpf = lerCpfValido(scanner);
                    if (novoCpf.equals("CANCELAR")) continue;

                    if (funcCpf.contains(novoCpf)) {
                        System.out.println(">>> ERRO: Este CPF ja esta cadastrado. <<<\n");
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

                    System.out.println("\n[OK] Sucesso! Funcionario cadastrado.");
                    System.out.println("Senha gerada: " + senhaGerada + " | Salario: " + formatarMoeda(salarioPadrao) + "\n");
                } catch (Exception e) {
                    System.out.println(">>> ERRO: Falha ao cadastrar. <<<\n");
                }
            } else if (op == 3) {
                System.out.println("\n================================================================");
                System.out.println("               ATUALIZACAO DE SALARIO DE FUNCIONARIO            ");
                System.out.println("================================================================");
                for (int i = 0; i < funcNome.size(); i++) {
                    System.out.printf("[%d] %s (%s) - Salario: %s%s\n",
                            (i + 1), funcNome.get(i), funcCargo.get(i),
                            formatarMoeda(funcSalario.get(i)),
                            verificarAlertaMediaSalarial(funcCargo.get(i), funcSalario.get(i)));
                }
                System.out.println("================================================================");
                int idx = lerInteiroValido(scanner, "Informe o numero (indice) do funcionario (0 para voltar): ") - 1;
                if (idx == -1000) continue;
                if (idx >= 0 && idx < funcNome.size()) {
                    double nSal = lerDoubleValido(scanner, "Informe o novo valor do salario (R$): ");
                    if (nSal == -999.0) continue;
                    funcSalario.set(idx, nSal);
                    System.out.println("[OK] Sucesso! Salario atualizado." + verificarAlertaMediaSalarial(funcCargo.get(idx), nSal) + "\n");
                } else {
                    System.out.println(">>> ERRO: Funcionario nao encontrado. <<<\n");
                }
            } else if (op == 4) {
                System.out.println("\n--- LISTA DE FUNCIONARIOS PARA EXCLUSAO ---");
                for (int i = 0; i < funcNome.size(); i++) {
                    System.out.printf("[%d] ID: %s | %s\n", (i + 1), funcId.get(i), funcNome.get(i));
                }
                int idx = lerInteiroValido(scanner, "\nInforme o numero (indice) do funcionario que deseja excluir (0 para voltar): ") - 1;
                if (idx == -1000) continue;
                if (idx >= 0 && idx < funcNome.size()) {
                    funcId.remove(idx);
                    funcNome.remove(idx);
                    funcCpf.remove(idx);
                    funcCargo.remove(idx);
                    funcSenha.remove(idx);
                    funcSalario.remove(idx);
                    funcPago.remove(idx);
                    System.out.println("[OK] Sucesso! Funcionario removido.\n");
                } else {
                    System.out.println(">>> ERRO: Funcionario nao encontrado. <<<\n");
                }
            }
        }
    }

    /* =========================================================================
       [MÓDULO] GESTÃO DE FORNECEDORES
       ========================================================================= */
    public static void menuGerenciarFornecedores(Scanner scanner, List<String> fornNome, List<String> fornCnpj,
                                                 List<String> fornUf, List<String> fornProdutoFornecido,
                                                 List<Double> fornPrecoPago, List<Double> fornLucroPretendidoPct,
                                                 List<Boolean> fornPago) {
        boolean rodando = true;
        while (rodando) {
            System.out.println("\n================================================================");
            System.out.println("                   PAINEL: GESTAO DE FORNECEDORES               ");
            System.out.println("================================================================");
            System.out.println("[1] Listar fornecedores cadastrados");
            System.out.println("[2] Cadastrar novo fornecedor");
            System.out.println("[3] Alterar preco de custo do fornecedor");
            System.out.println("[4] Excluir fornecedor");
            System.out.println("[5] Voltar ao menu administrativo anterior");
            System.out.println("================================================================");

            int op = lerInteiroValido(scanner, "Selecione uma opcao (0 para voltar): ");

            if (op == 5 || op == -999) {
                rodando = false;
            } else if (op == 1) {
                System.out.println("\n================================================================");
                System.out.println("                 LISTA DE FORNECEDORES ATIVOS                   ");
                System.out.println("================================================================");
                for (int i = 0; i < fornNome.size(); i++) {
                    System.out.printf("[%d] Razao Social: %s | CNPJ: %s | UF: %s | Produto: %s\n",
                            (i + 1), fornNome.get(i), fornCnpj.get(i), fornUf.get(i), fornProdutoFornecido.get(i));
                }
                System.out.println("================================================================");
            } else if (op == 2) {
                try {
                    String nomeForn = lerTextoApenasLetras(scanner, "Razao Social / Nome [Ex: Suplemax Ltda] (0 para voltar): ");
                    if (nomeForn.equals("CANCELAR")) continue;

                    String cnpjForn = lerCnpjValido(scanner);
                    if (cnpjForn.equals("CANCELAR")) continue;

                    if (fornCnpj.contains(cnpjForn)) {
                        System.out.println(">>> ERRO: Este CNPJ ja esta cadastrado. <<<\n");
                        continue;
                    }

                    String uf = lerUfValida(scanner, "UF de origem [Ex: SP]: ");
                    if (uf.equals("CANCELAR")) continue;

                    System.out.print("Produto fornecido [Ex: Whey Protein] (0 para voltar): ");
                    String prod = scanner.nextLine().trim().toUpperCase();
                    if (prod.equals("0") || prod.equalsIgnoreCase("SAIR")) continue;

                    double custo = lerDoubleValido(scanner, "Preco pago ao fornecedor (Custo R$): ");
                    if (custo == -999.0) continue;

                    double lucro = lerDoubleValido(scanner, "Lucro pretendido para o mercado (%): ");
                    if (lucro == -999.0) continue;

                    fornNome.add(nomeForn);
                    fornCnpj.add(cnpjForn);
                    fornUf.add(uf);
                    fornProdutoFornecido.add(prod);
                    fornPrecoPago.add(custo);
                    fornLucroPretendidoPct.add(lucro);
                    fornPago.add(false);

                    System.out.println("[OK] Sucesso! Fornecedor cadastrado.\n");
                } catch (Exception e) {
                    System.out.println(">>> ERRO: Falha ao registrar fornecedor. <<<\n");
                }
            } else if (op == 3) {
                System.out.println("\n================================================================");
                System.out.println("               ATUALIZACAO DE CUSTO DE FORNECEDOR               ");
                System.out.println("================================================================");
                for (int i = 0; i < fornNome.size(); i++) {
                    System.out.printf("[%d] %s (Custo: %s)\n", (i + 1), fornNome.get(i), formatarMoeda(fornPrecoPago.get(i)));
                }
                System.out.println("================================================================");
                int idx = lerInteiroValido(scanner, "Informe o numero (indice) do fornecedor (0 para voltar): ") - 1;
                if (idx == -1000) continue;
                if (idx >= 0 && idx < fornNome.size()) {
                    double nCusto = lerDoubleValido(scanner, "Informe o novo preco de custo (R$): ");
                    if (nCusto == -999.0) continue;
                    fornPrecoPago.set(idx, nCusto);
                    System.out.println("[OK] Sucesso! Preco de custo atualizado.\n");
                } else {
                    System.out.println(">>> ERRO: Fornecedor nao encontrado. <<<\n");
                }
            } else if (op == 4) {
                System.out.println("\n--- LISTA DE FORNECEDORES PARA EXCLUSAO ---");
                for (int i = 0; i < fornNome.size(); i++) {
                    System.out.printf("[%d] %s | CNPJ: %s\n", (i + 1), fornNome.get(i), fornCnpj.get(i));
                }
                int idx = lerInteiroValido(scanner, "\nInforme o numero (indice) do fornecedor que deseja excluir (0 para voltar): ") - 1;
                if (idx == -1000) continue;
                if (idx >= 0 && idx < fornNome.size()) {
                    fornNome.remove(idx);
                    fornCnpj.remove(idx);
                    fornUf.remove(idx);
                    fornProdutoFornecido.remove(idx);
                    fornPrecoPago.remove(idx);
                    fornLucroPretendidoPct.remove(idx);
                    fornPago.remove(idx);
                    System.out.println("[OK] Sucesso! Fornecedor removido.\n");
                } else {
                    System.out.println(">>> ERRO: Fornecedor nao encontrado. <<<\n");
                }
            }
        }
    }

    /* =========================================================================
       [MÓDULO] FINANCEIRO, ESTOQUE, TRIBUTOS E CENTRAL DE PAGAMENTOS
       ========================================================================= */
    public static void menuFinanceiroEOperacional(Scanner scanner, List<String> prodNome, List<String> prodNcm,
                                                  List<Double> prodPrecos, List<Double> prodPrecoOriginal,
                                                  List<Integer> prodEstoque, List<String> funcNome,
                                                  List<String> funcCargo, List<Double> funcSalario,
                                                  List<Boolean> funcPago, List<String> fornNome,
                                                  List<String> fornProdutoFornecido, List<Double> fornPrecoPago,
                                                  List<Boolean> fornPago) {
        boolean rodando = true;
        while (rodando) {
            System.out.println("\n================================================================");
            System.out.println("             PAINEL: FINANCEIRO, ESTOQUE E TRIBUTOS             ");
            System.out.println("================================================================");
            System.out.println("[1] Listar produtos e fichas fiscais");
            System.out.println("[2] Cadastrar novo produto e ficha fiscal");
            System.out.println("[3] Ajustar precos de produtos");
            System.out.println("[4] Registrar movimentacao de estoque / compras");
            System.out.println("[5] Configurar ano fiscal dos tributos");
            System.out.println("[6] Exibir balanco financeiro e demonstrativo DRE");
            System.out.println("[7] Central de Pagamentos (Salarios, Fornecedores e Impostos)");
            System.out.println("[8] Excluir produto do catalogo");
            System.out.println("[9] Voltar ao menu administrativo anterior");
            System.out.println("================================================================");

            int op = lerInteiroValido(scanner, "Selecione uma opcao (0 para voltar): ");

            if (op == 9 || op == -999) {
                rodando = false;
            } else if (op == 1) {
                System.out.println("\n================================================================");
                System.out.println("                  CATALOGO DE PRODUTOS E TRIBUTOS               ");
                System.out.println("================================================================");
                for (int i = 0; i < prodNome.size(); i++) {
                    if (prodNome.get(i) != null) {
                        System.out.printf("[%d] Produto: %s | Preco: %s | Estoque: %d | NCM: %s\n",
                                (i + 1), prodNome.get(i), formatarMoeda(prodPrecos.get(i)),
                                prodEstoque.get(i), prodNcm.get(i));
                    }
                }
                System.out.println("================================================================");
            } else if (op == 2) {
                try {
                    int idPr = lerInteiroValido(scanner, "Informe o ID/Posicao desejada para o produto (0 para voltar): ") - 1;
                    if (idPr == -1000) continue;

                    if (idPr >= 0) {
                        System.out.print("Nome do produto [Ex: Vitamina C] (0 para voltar): ");
                        String nProd = scanner.nextLine().trim().toUpperCase();
                        if (nProd.equals("0") || nProd.equalsIgnoreCase("SAIR")) continue;

                        if (prodNome.contains(nProd)) {
                            System.out.println(">>> ERRO: Produto ja cadastrado. <<<\n");
                            continue;
                        }

                        double pVenda = lerDoubleValido(scanner, "Preco de venda (R$): ");
                        if (pVenda == -999.0) continue;

                        int eIni = lerInteiroValido(scanner, "Quantidade em estoque inicial (0 para voltar): ");
                        if (eIni == -999) continue;

                        System.out.print("Codigo NCM [Ex: 2106.90.30] (0 para voltar): ");
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
                        System.out.println("[OK] Sucesso! Produto cadastrado.\n");
                    }
                } catch (Exception e) {
                    System.out.println(">>> ERRO: Falha ao cadastrar. <<<\n");
                }
            } else if (op == 3) {
                if (!prodNome.isEmpty()) {
                    System.out.println("\n================================================================");
                    System.out.println("                  TABELA DE PRECOS ATUAIS                       ");
                    System.out.println("================================================================");
                    for (int i = 0; i < prodNome.size(); i++) {
                        if (prodNome.get(i) != null) {
                            System.out.println("ID: " + (i + 1) + " | " + prodNome.get(i) + " | Preco: " + formatarMoeda(prodPrecos.get(i)));
                        }
                    }
                    System.out.println("================================================================");
                    int idPr = lerInteiroValido(scanner, "Informe o ID do produto para alterar preco (0 para voltar): ") - 1;
                    if (idPr == -1000) continue;
                    if (idPr >= 0 && idPr < prodNome.size() && prodNome.get(idPr) != null) {
                        double nP = lerDoubleValido(scanner, "Informe o novo preco fixo (R$): ");
                        if (nP == -999.0) continue;
                        prodPrecos.set(idPr, nP);
                        System.out.println("[OK] Sucesso! Preco alterado.\n");
                    }
                }
            } else if (op == 4) {
                for (int i = 0; i < prodNome.size(); i++) {
                    if (prodNome.get(i) != null) {
                        System.out.println("ID: " + (i + 1) + " | " + prodNome.get(i) + " | Estoque atual: " + prodEstoque.get(i));
                    }
                }
                int pI = lerInteiroValido(scanner, "\nInforme o ID do produto para reposicao (0 para voltar): ") - 1;
                if (pI == -1000) continue;
                if (pI >= 0 && pI < prodNome.size() && prodNome.get(pI) != null) {
                    int qE = lerInteiroValido(scanner, "Quantidade de entrada no estoque (0 para voltar): ");
                    if (qE == -999) continue;
                    double cT = lerDoubleValido(scanner, "Custo total pago pelo lote (R$): ");
                    if (cT == -999.0) continue;

                    if (saldoCaixaLoja < cT) {
                        System.out.println(">>> ERRO: Saldo em caixa insuficiente para esta compra de estoque. <<<\n");
                        continue;
                    }
                    prodEstoque.set(pI, prodEstoque.get(pI) + qE);
                    totalGastosEstoque += cT;
                    saldoCaixaLoja -= cT;
                    System.out.println("[OK] Sucesso! Estoque abastecido. Custo registrado e caixa atualizado.\n");
                }
            } else if (op == 5) {
                int novoAno = lerInteiroValido(scanner, "Informe o novo ano fiscal de referencia [Ex: 2027] (0 para voltar): ");
                if (novoAno != -999) {
                    if (novoAno == 2026 || novoAno == 2027) {
                        anoExercicioPadrao = novoAno;
                        System.out.println("[OK] Sucesso! Ano fiscal alterado para " + anoExercicioPadrao + ".\n");
                    } else {
                        System.out.println(">>> ERRO: Informe 2026 ou 2027. Ano mantido. <<<\n");
                    }
                }
            } else if (op == 6) {
                double lucroBruto = totalReceitaVendas - totalGastosEstoque;
                double lucroOperacional = lucroBruto - totalSalariosPagos;
                double impostosSobreLucro = (lucroOperacional > 0) ? (lucroOperacional * 0.15) : 0.0;
                double lucroLiquidoFinal = lucroOperacional - impostosSobreLucro;

                System.out.println("\n================================================================");
                System.out.println("            DEMONSTRATIVO DO RESULTADO DO EXERCICIO (DRE)       ");
                System.out.println("================================================================");
                System.out.println(" (*) Saldo atual em caixa: " + formatarMoeda(saldoCaixaLoja));
                System.out.println(" (+) Receita bruta de vendas: " + formatarMoeda(totalReceitaVendas));
                System.out.println(" (-) Custos totais de estoque: " + formatarMoeda(totalGastosEstoque));
                System.out.println("----------------------------------------------------------------");
                System.out.println(" (=) Lucro Bruto: " + formatarMoeda(lucroBruto));
                System.out.println(" (-) Gastos com folha salarial: " + formatarMoeda(totalSalariosPagos));
                System.out.println("----------------------------------------------------------------");
                System.out.println(" (=) Lucro Operacional: " + formatarMoeda(lucroOperacional));
                System.out.println(" (-) Provisao de Impostos (~15%): " + formatarMoeda(impostosSobreLucro));
                System.out.println("----------------------------------------------------------------");
                System.out.println(" (*) LUCRO LIQUIDO FINAL: " + formatarMoeda(lucroLiquidoFinal));
                System.out.println("================================================================");
            } else if (op == 7) {
                boolean pagando = true;
                while (pagando) {
                    System.out.println("\n================================================================");
                    System.out.println("         CENTRAL DE PAGAMENTOS E OBRIGACOES FINANCEIRAS         ");
                    System.out.println("================================================================");
                    System.out.println(" (*) Saldo atual em caixa: " + formatarMoeda(saldoCaixaLoja));

                    double folhaPend = 0.0;
                    for (int i = 0; i < funcSalario.size(); i++) {
                        if (!funcPago.get(i)) folhaPend += funcSalario.get(i);
                    }
                    double fornPend = 0.0;
                    for (int i = 0; i < fornPrecoPago.size(); i++) {
                        if (!fornPago.get(i)) fornPend += fornPrecoPago.get(i);
                    }

                    System.out.println(" Total de Salarios Pendentes: " + formatarMoeda(folhaPend));
                    System.out.println(" Total de Fornecedores Pendentes: " + formatarMoeda(fornPend));
                    System.out.println(" Total de Impostos Pendentes: " + formatarMoeda(impostosPendentesTotal));
                    System.out.println("----------------------------------------------------------------");
                    System.out.println("[1] Pagar Salarios (Listar, Pagar Especifico ou Todos)");
                    System.out.println("[2] Pagar Fornecedores (Listar, Pagar Especifico ou Todos)");
                    System.out.println("[3] Pagar Impostos da Receita Federal");
                    System.out.println("[4] Voltar ao menu financeiro anterior");
                    System.out.println("================================================================");

                    int opPag = lerInteiroValido(scanner, "Selecione uma opcao (0 para voltar): ");

                    if (opPag == 4 || opPag == -999) {
                        pagando = false;
                    } else if (opPag == 1) {
                        System.out.println("\n================================================================");
                        System.out.println("                   PAGAMENTO DE SALARIOS                        ");
                        System.out.println("================================================================");
                        for (int i = 0; i < funcNome.size(); i++) {
                            String st = funcPago.get(i) ? "PAGO" : "PENDENTE";
                            String alerta = verificarAlertaMediaSalarial(funcCargo.get(i), funcSalario.get(i));
                            System.out.printf("[%d] %s (%s) - Valor: %s [%s]%s\n",
                                    (i + 1), funcNome.get(i), funcCargo.get(i),
                                    formatarMoeda(funcSalario.get(i)), st, alerta);
                        }
                        System.out.println("================================================================");
                        System.out.println("[1] Pagar funcionario especifico | [2] Pagar todos de uma vez | [3] Voltar");
                        int subS = lerInteiroValido(scanner, "Escolha uma opcao (0 para voltar): ");
                        if (subS == -999 || subS == 3) continue;

                        if (subS == 1) {
                            int idxF = lerInteiroValido(scanner, "Informe o numero (ID) do funcionario (0 para voltar): ") - 1;
                            if (idxF == -1000) continue;
                            if (idxF >= 0 && idxF < funcNome.size()) {
                                if (funcPago.get(idxF)) {
                                    System.out.println("Este salario ja foi quitado.");
                                } else {
                                    double val = funcSalario.get(idxF);
                                    if (saldoCaixaLoja >= val) {
                                        saldoCaixaLoja -= val;
                                        totalSalariosPagos += val;
                                        funcPago.set(idxF, true);
                                        System.out.println("[OK] Salario de " + funcNome.get(idxF) + " pago com sucesso!");
                                    } else {
                                        System.out.println(">>> ERRO: Caixa insuficiente. <<<");
                                    }
                                }
                            } else {
                                System.out.println(">>> ERRO: Funcionario invalido. <<<");
                            }
                        } else if (subS == 2) {
                            if (folhaPend <= 0) {
                                System.out.println("Nao ha salarios pendentes.");
                            } else if (saldoCaixaLoja >= folhaPend) {
                                saldoCaixaLoja -= folhaPend;
                                totalSalariosPagos += folhaPend;
                                for (int i = 0; i < funcPago.size(); i++) {
                                    funcPago.set(i, true);
                                }
                                System.out.println("[OK] Todos os salarios pendentes foram quitados com sucesso!");
                            } else {
                                System.out.println(">>> ERRO: Caixa insuficiente para quitar a folha completa. <<<");
                            }
                        }
                    } else if (opPag == 2) {
                        System.out.println("\n================================================================");
                        System.out.println("                 PAGAMENTO DE FORNECEDORES                      ");
                        System.out.println("================================================================");
                        for (int i = 0; i < fornNome.size(); i++) {
                            String st = fornPago.get(i) ? "PAGO" : "PENDENTE";
                            System.out.printf("[%d] %s - Produto: %s - Valor Custo: %s [%s]\n",
                                    (i + 1), fornNome.get(i), fornProdutoFornecido.get(i),
                                    formatarMoeda(fornPrecoPago.get(i)), st);
                        }
                        System.out.println("================================================================");
                        System.out.println("[1] Pagar fornecedor especifico | [2] Pagar todos de uma vez | [3] Voltar");
                        int subF = lerInteiroValido(scanner, "Escolha uma opcao (0 para voltar): ");
                        if (subF == -999 || subF == 3) continue;

                        if (subF == 1) {
                            int idxFor = lerInteiroValido(scanner, "Informe o numero (ID) do fornecedor (0 para voltar): ") - 1;
                            if (idxFor == -1000) continue;
                            if (idxFor >= 0 && idxFor < fornNome.size()) {
                                if (fornPago.get(idxFor)) {
                                    System.out.println("Este fornecedor ja foi pago.");
                                } else {
                                    double valF = fornPrecoPago.get(idxFor);
                                    if (saldoCaixaLoja >= valF) {
                                        saldoCaixaLoja -= valF;
                                        totalFornecedoresPagos += valF;
                                        fornPago.set(idxFor, true);
                                        System.out.println("[OK] Fornecedor " + fornNome.get(idxFor) + " pago com sucesso!");
                                    } else {
                                        System.out.println(">>> ERRO: Caixa insuficiente. <<<");
                                    }
                                }
                            } else {
                                System.out.println(">>> ERRO: Fornecedor invalido. <<<");
                            }
                        } else if (subF == 2) {
                            if (fornPend <= 0) {
                                System.out.println("Nao ha fornecedores pendentes.");
                            } else if (saldoCaixaLoja >= fornPend) {
                                saldoCaixaLoja -= fornPend;
                                totalFornecedoresPagos += fornPend;
                                for (int i = 0; i < fornPago.size(); i++) {
                                    fornPago.set(i, true);
                                }
                                System.out.println("[OK] Todos os fornecedores pendentes foram pagos com sucesso!");
                            } else {
                                System.out.println(">>> ERRO: Caixa insuficiente para quitar todos os fornecedores. <<<");
                            }
                        }
                    } else if (opPag == 3) {
                        System.out.println("\n--- QUITACAO DE IMPOSTOS ---");
                        System.out.println("Total de Impostos Pendentes: " + formatarMoeda(impostosPendentesTotal));
                        if (impostosPendentesTotal <= 0) {
                            System.out.println("Nao ha impostos pendentes para pagamento.");
                        } else {
                            System.out.print("Deseja quitar os impostos agora? [S/N] (0 para voltar): ");
                            String respImp = scanner.nextLine().trim();
                            if (respImp.equalsIgnoreCase("0") || respImp.equalsIgnoreCase("SAIR")) continue;
                            if (respImp.equalsIgnoreCase("S")) {
                                if (saldoCaixaLoja >= impostosPendentesTotal) {
                                    saldoCaixaLoja -= impostosPendentesTotal;
                                    impostosPendentesTotal = 0.0;
                                    System.out.println("[OK] Impostos quitados com sucesso junto a Receita Federal!");
                                } else {
                                    System.out.println(">>> ERRO: Caixa insuficiente para quitar os impostos. <<<");
                                }
                            }
                        }
                    }
                }
            } else if (op == 8) {
                System.out.println("\n--- LISTA DE PRODUTOS PARA EXCLUSAO ---");
                for (int i = 0; i < prodNome.size(); i++) {
                    if (prodNome.get(i) != null) {
                        System.out.printf("[%d] %s\n", (i + 1), prodNome.get(i));
                    }
                }
                int idx = lerInteiroValido(scanner, "\nInforme o numero (ID) do produto que deseja excluir (0 para voltar): ") - 1;
                if (idx == -1000) continue;
                if (idx >= 0 && idx < prodNome.size() && prodNome.get(idx) != null) {
                    prodNome.set(idx, null);
                    prodNcm.set(idx, "");
                    prodPrecos.set(idx, 0.0);
                    prodPrecoOriginal.set(idx, 0.0);
                    prodEstoque.set(idx, 0);
                    System.out.println("[OK] Sucesso! Produto removido do catalogo.\n");
                } else {
                    System.out.println(">>> ERRO: Produto nao encontrado. <<<\n");
                }
            }
        }
    }

    /* =========================================================================
       MÉTODO PRINCIPAL (MAIN)
       ========================================================================= */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // ===================== FUNCIONARIOS =====================
        List<String> funcId = new ArrayList<>(Arrays.asList("F01", "F02", "F03"));
        List<String> funcNome = new ArrayList<>(Arrays.asList("CARLOS GERENTE", "ANA OPERADORA", "MARINA FINANCEIRO"));
        List<String> funcCpf = new ArrayList<>(Arrays.asList("80695789554", "89246912950", "52998224725"));
        List<String> funcCargo = new ArrayList<>(Arrays.asList("GERENTE", "OPERADOR", "FINANCEIRO"));
        List<String> funcSenha = new ArrayList<>(Arrays.asList("admin123", "op123", "fin123"));
        List<Double> funcSalario = new ArrayList<>(Arrays.asList(5500.00, 2200.00, 4800.00));
        List<Boolean> funcPago = new ArrayList<>(Arrays.asList(false, false, false));

        // ===================== CLIENTES =====================
        List<String> cliNomeArr = new ArrayList<>(Arrays.asList("MARIO SILVA"));
        List<String> cliCpfArr = new ArrayList<>(Arrays.asList("11144477735"));
        List<String> cliSenhaArr = new ArrayList<>(Arrays.asList("cli123"));
        List<String> cliSexoArr = new ArrayList<>(Arrays.asList("M"));
        List<String> cliDataNascArr = new ArrayList<>(Arrays.asList("10/05/1995"));
        List<String> cliUfArr = new ArrayList<>(Arrays.asList("SP"));
        List<String> cliCepArr = new ArrayList<>(Arrays.asList("04753060"));

        // ===================== PRODUTOS =====================
        List<String> prodNome = new ArrayList<>(Arrays.asList(
            "WHEY PROTEIN ISOLADO", "BEBIDA ISOTONICA 500ML", "CAMISA ESPORTIVA DRY-FIT",
            "TENIS DE CORRIDA PERFORMANCE", "CREATINA MONOHIDRATADA 250G", "BARRA DE PROTEINA CHOCOLATE",
            "ENERGETICO LATA 473ML", "AGUA MINERAL 500ML SEM GAS", "BERMUDA TERMICA COMPRESSAO",
            "MEIAS ESPORTIVAS CANO ALTO (3UN)", "CHINELO SLIDE ANATOMICO", "MOCHILA ESPORTIVA IMPERMEAVEL",
            "GARRAFA SQUEEZE 1L INOX", "LUVA DE TREINO MUSCULACAO"
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

        // ===================== FORNECEDORES =====================
        List<String> fornNome = new ArrayList<>(Arrays.asList("SUPLEMAX DISTRIBUIDORA LTDA"));
        List<String> fornCnpj = new ArrayList<>(Arrays.asList("12345678000190"));
        List<String> fornUf = new ArrayList<>(Arrays.asList("SP"));
        List<String> fornProdutoFornecido = new ArrayList<>(Arrays.asList("WHEY PROTEIN ISOLADO"));
        List<Double> fornPrecoPago = new ArrayList<>(Arrays.asList(80.00));
        List<Double> fornLucroPretendidoPct = new ArrayList<>(Arrays.asList(50.0));
        List<Boolean> fornPago = new ArrayList<>(Arrays.asList(false));

        boolean sistemaAtivo = true;
        while (sistemaAtivo) {
            exibirCabecalho();
            System.out.println("SELECIONE O SEU PERFIL DE ACESSO:");
            System.out.println("[1] CLIENTE (Catalogo de compras e carrinho)");
            System.out.println("[2] FUNCIONARIO (Painel administrativo e login)");
            System.out.println("[3] SAIR DO SISTEMA");

            int perfilAcesso = lerInteiroValido(scanner, "\nDigite a opcao desejada: ");

            if (perfilAcesso == 3 || perfilAcesso == -999) {
                sistemaAtivo = false;
                break;
            } else if (perfilAcesso < 1 || perfilAcesso > 2) {
                System.out.println(">>> ERRO: Opcao invalida. <<<\n");
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
                    System.out.println("[1] Visualizar catalogo de produtos");
                    System.out.println("[2] Adicionar produto ao carrinho de compras");
                    System.out.println("[3] Visualizar carrinho / Revisar / Finalizar compra");
                    System.out.println("[4] Voltar a pagina anterior (Trocar perfil)");
                    System.out.println("[5] Voltar ao inicio do sistema");
                    System.out.println("================================================================");

                    int opcaoCliente = lerInteiroValido(scanner, "Selecione uma opcao (0 para voltar): ");

                    if (opcaoCliente == 4 || opcaoCliente == -999) {
                        break;
                    } else if (opcaoCliente == 5) {
                        loopCliente = false;
                        continue;
                    }

                    // Identificacao so quando for interagir com o carrinho
                    if ((opcaoCliente == 2 || opcaoCliente == 3) && !clienteIdentificado) {
                        System.out.println("\n================================================================");
                        System.out.println("                   IDENTIFICACAO DO CLIENTE                     ");
                        System.out.println("================================================================");
                        cliNome = lerTextoApenasLetras(scanner, "Nome completo [Ex: Maria Souza] (0 para voltar): ");
                        if (cliNome.equals("CANCELAR")) continue;

                        cliCpf = lerCpfValido(scanner);
                        if (cliCpf.equals("CANCELAR")) continue;
                        System.out.println("================================================================");

                        int clienteIdx = cliCpfArr.indexOf(cliCpf);

                        if (clienteIdx != -1) {
                            System.out.println("CPF ja cadastrado (" + cliNomeArr.get(clienteIdx) + ").");
                            boolean senhaCorreta = false;
                            int tentativasSenha = 3;

                            while (tentativasSenha > 0 && !senhaCorreta) {
                                System.out.print("Digite sua senha de acesso (0 para voltar): ");
                                String senhaDigitada = scanner.nextLine().trim();

                                if (senhaDigitada.equals("0") || senhaDigitada.equalsIgnoreCase("SAIR")) {
                                    break;
                                }
                                if (cliSenhaArr.get(clienteIdx).equals(senhaDigitada)) {
                                    senhaCorreta = true;
                                    cliNome = cliNomeArr.get(clienteIdx);
                                    cliUf = cliUfArr.get(clienteIdx);
                                    cliCep = cliCepArr.get(clienteIdx);
                                    clienteCadastrado = true;
                                    System.out.println("[OK] Senha correta! Acesso liberado.");
                                } else {
                                    tentativasSenha--;
                                    if (tentativasSenha > 0) {
                                        System.out.println(">>> SENHA INCORRETA. Restam " + tentativasSenha + " tentativa(s). <<<");
                                    } else {
                                        System.out.println(">>> Limite de tentativas excedido. <<<\n");
                                    }
                                }
                            }
                            if (!senhaCorreta) continue;
                        } else {
                            int optCliNovo = lerInteiroValido(scanner, "CPF nao cadastrado. [1 - Cadastrar agora | 2 - Comprar sem cadastro] (0 para voltar): ");
                            if (optCliNovo == -999) continue;

                            if (optCliNovo == 1) {
                                System.out.println("\n================================================================");
                                System.out.println("                 CADASTRO RAPIDO DO CLIENTE                     ");
                                System.out.println("================================================================");
                                String sexoCli = lerTextoApenasLetras(scanner, "Sexo [Ex: M ou F] (0 para voltar): ");
                                if (sexoCli.equals("CANCELAR")) continue;

                                System.out.print("Data de nascimento [Ex: 10/05/1995] (0 para voltar): ");
                                String nascCli = scanner.nextLine().trim();
                                if (nascCli.equals("0") || nascCli.equalsIgnoreCase("SAIR")) continue;

                                try {
                                    LocalDate.parse(nascCli, DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT));
                                } catch (DateTimeException e) {
                                    System.out.println(">>> Data invalida. Usando 01/01/2000 como padrao. <<<");
                                    nascCli = "01/01/2000";
                                }

                                cliUf = lerUfValida(scanner, "UF de residencia [Ex: SP]: ");
                                if (cliUf.equals("CANCELAR")) continue;

                                cliCep = lerCepValido(scanner);
                                if (cliCep.equals("CANCELAR")) continue;

                                String cliSenha = geradorSenhaAleatoriaSistema();
                                System.out.println("================================================================");
                                System.out.println("Senha gerada para futuros acessos: " + cliSenha);

                                cliNomeArr.add(cliNome);
                                cliCpfArr.add(cliCpf);
                                cliSenhaArr.add(cliSenha);
                                cliSexoArr.add(sexoCli);
                                cliDataNascArr.add(nascCli);
                                cliUfArr.add(cliUf);
                                cliCepArr.add(cliCep);
                                clienteCadastrado = true;
                            } else {
                                clienteCadastrado = false;
                            }
                        }
                        clienteIdentificado = true;
                        System.out.println("\nBem-vindo(a), " + cliNome.toUpperCase() + "!");
                    }

                    if (opcaoCliente == 1) {
                        System.out.println("\n================================================================");
                        System.out.println("                  CATALOGO DE PRODUTOS DISPONIVEIS              ");
                        System.out.println("================================================================");
                        for (int i = 0; i < prodNome.size(); i++) {
                            if (prodNome.get(i) != null) {
                                String statusEstoque = (prodEstoque.get(i) > 0) ? " | Estoque: " + prodEstoque.get(i) : " | [ESGOTADO]";
                                String tagOferta = (prodPrecos.get(i) < prodPrecoOriginal.get(i)) ? " ** [OFERTA] **" : "";
                                System.out.println("ID: " + (i + 1) + " | " + prodNome.get(i) + " | Preco: " +
                                        formatarMoeda(prodPrecos.get(i)) + tagOferta + statusEstoque);
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
                                System.out.println("ID: " + (i + 1) + " | " + prodNome.get(i) + " - " +
                                        formatarMoeda(prodPrecos.get(i)) + " (Disp: " + prodEstoque.get(i) + ")");
                                temProd = true;
                            }
                        }
                        System.out.println("================================================================");

                        if (!temProd) {
                            System.out.println("Nenhum produto disponivel em estoque.");
                        } else {
                            int posItem = lerInteiroValido(scanner, "Digite o ID do produto desejado (0 para voltar): ") - 1;
                            if (posItem == -1000) continue;

                            if (posItem >= 0 && posItem < prodNome.size() && prodNome.get(posItem) != null && prodEstoque.get(posItem) > 0) {
                                int qtde = lerInteiroValido(scanner, "Digite a quantidade desejada (0 para voltar): ");
                                if (qtde == -999) continue;

                                int posNoCarrinho = carrinhoProdIndex.indexOf(posItem);
                                int qtdeJaNoCarrinho = (posNoCarrinho >= 0) ? carrinhoQtde.get(posNoCarrinho) : 0;

                                if (qtde > 0 && (qtdeJaNoCarrinho + qtde) <= prodEstoque.get(posItem)) {
                                    if (posNoCarrinho >= 0) {
                                        carrinhoQtde.set(posNoCarrinho, qtdeJaNoCarrinho + qtde);
                                        System.out.println("[OK] Quantidade do produto atualizada no carrinho!");
                                    } else {
                                        carrinhoProdIndex.add(posItem);
                                        carrinhoQtde.add(qtde);
                                        System.out.println("[OK] Produto adicionado ao carrinho!");
                                    }
                                } else {
                                    System.out.println(">>> ERRO: Quantidade invalida ou superior ao estoque (" +
                                            prodEstoque.get(posItem) + "). <<<");
                                }
                            } else {
                                System.out.println(">>> ERRO: ID invalido. <<<");
                            }
                        }
                    } else if (opcaoCliente == 3) {
                        if (carrinhoProdIndex.isEmpty()) {
                            System.out.println("\nSeu carrinho de compras esta vazio.");
                        } else {
                            boolean revisando = true;
                            while (revisando) {
                                double valCarrinho = 0.0;
                                System.out.println("\n================================================================");
                                System.out.println("                 REVISAO DO CARRINHO DE COMPRAS                 ");
                                System.out.println("================================================================");
                                for (int i = 0; i < carrinhoProdIndex.size(); i++) {
                                    int idxP = carrinhoProdIndex.get(i);
                                    double valItem = prodPrecos.get(idxP) * carrinhoQtde.get(i);
                                    valCarrinho += valItem;
                                    System.out.printf("[%d] %s | Qtde: %d | Unitario: %s | Subtotal: %s\n",
                                            (i + 1), prodNome.get(idxP), carrinhoQtde.get(i),
                                            formatarMoeda(prodPrecos.get(idxP)), formatarMoeda(valItem));
                                }
                                System.out.println("----------------------------------------------------------------");
                                System.out.println("VALOR TOTAL DO CARRINHO: " + formatarMoeda(valCarrinho));
                                System.out.println("================================================================");

                                System.out.println("[1] Concluir compra | [2] Alterar quantidade | [3] Remover item | [4] Voltar");
                                System.out.println("================================================================");
                                int acao = lerInteiroValido(scanner, "Escolha uma opcao (0 para voltar): ");
                                if (acao == -999 || acao == 4) {
                                    revisando = false;
                                    continue;
                                }

                                if (acao == 1) {
                                    if (!clienteCadastrado) {
                                        System.out.println("\n================================================================");
                                        System.out.println("             CADASTRO RAPIDO PARA FATURAMENTO                   ");
                                        System.out.println("================================================================");
                                        String sexoC = lerTextoApenasLetras(scanner, "Sexo [Ex: M] (0 para voltar): ");
                                        if (sexoC.equals("CANCELAR")) continue;

                                        System.out.print("Data de nascimento [Ex: 10/05/1995] (0 para voltar): ");
                                        String nascC = scanner.nextLine().trim();
                                        if (nascC.equals("0") || nascC.equalsIgnoreCase("SAIR")) continue;

                                        cliUf = lerUfValida(scanner, "UF de residencia [Ex: SP]: ");
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
                                        System.out.println("Cadastro efetivado! Senha: " + senhaGerada);
                                    }

                                    int pagto = lerInteiroValido(scanner, "Forma de Pagamento [1 - PIX (10% Desc) | 2 - Credito a Vista | 3 - Parcelado 3x (+5%)] (0 para voltar): ");
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
                                    int idxA = lerInteiroValido(scanner, "Numero do item no carrinho para alterar (0 para voltar): ") - 1;
                                    if (idxA == -1000) continue;
                                    if (idxA >= 0 && idxA < carrinhoProdIndex.size()) {
                                        int nQtde = lerInteiroValido(scanner, "Nova quantidade desejada: ");
                                        if (nQtde == -999) continue;
                                        int idxP = carrinhoProdIndex.get(idxA);
                                        if (nQtde > 0 && nQtde <= prodEstoque.get(idxP)) {
                                            carrinhoQtde.set(idxA, nQtde);
                                            System.out.println("[OK] Quantidade atualizada.");
                                        } else {
                                            System.out.println(">>> ERRO: Estoque insuficiente. <<<");
                                        }
                                    }
                                } else if (acao == 3) {
                                    int idxR = lerInteiroValido(scanner, "Numero do item para remover do carrinho (0 para voltar): ") - 1;
                                    if (idxR == -1000) continue;
                                    if (idxR >= 0 && idxR < carrinhoProdIndex.size()) {
                                        carrinhoProdIndex.remove(idxR);
                                        carrinhoQtde.remove(idxR);
                                        System.out.println("[OK] Item removido.");
                                    }
                                }
                            }
                        }
                    }
                }

            /* =========================================================================
               FLUXO DO FUNCIONARIO (PAINEL ADMINISTRATIVO UNIFICADO)
               ========================================================================= */
            } else if (perfilAcesso == 2) {
                System.out.println("\n----------------------------------------------------------------");
                System.out.println("                   LOGIN DO FUNCIONARIO                         ");
                System.out.println("----------------------------------------------------------------");
                String cpfLogin = lerCpfValido(scanner);
                if (cpfLogin.equals("CANCELAR")) continue;

                int funcLogadoIndex = funcCpf.indexOf(cpfLogin);

                if (funcLogadoIndex == -1) {
                    System.out.println(">>> ERRO: CPF nao cadastrado. <<<\n");
                    continue;
                }

                System.out.println("Usuario reconhecido: ID [" + funcId.get(funcLogadoIndex) + "] " +
                        funcNome.get(funcLogadoIndex) + " (" + funcCargo.get(funcLogadoIndex) + ")");

                int tentativas = 3;
                boolean acessoLiberado = false;
                while (tentativas > 0) {
                    System.out.print("Digite sua senha de funcionario (0 para voltar): ");
                    String senhaL = scanner.nextLine().trim();

                    if (senhaL.equals("0") || senhaL.equalsIgnoreCase("SAIR")) {
                        break;
                    }

                    if (funcSenha.get(funcLogadoIndex).equals(senhaL)) {
                        acessoLiberado = true;
                        break;
                    } else {
                        tentativas--;
                        if (tentativas > 0) {
                            System.out.println(">>> SENHA INCORRETA. Restam " + tentativas + " tentativa(s). <<<");
                        } else {
                            // Desbloqueio por autoridade superior (Gerente)
                            System.out.println("\n==================================================");
                            System.out.println(" >>> BLOQUEIO DE SEGURANCA: 3 TENTATIVAS FALHADAS! <<<");
                            System.out.println("==================================================");
                            System.out.println(" Para desbloquear, e necessaria a intervencao de um Gerente.");
                            System.out.print("DIGITE O ID DO GERENTE [Ex: F01]: ");
                            String idGerenteSup = scanner.nextLine().trim().toUpperCase();
                            System.out.print("DIGITE A SENHA DO GERENTE: ");
                            String senhaGerenteSup = scanner.nextLine();

                            int gerenciaIndex = -1;
                            for (int g = 0; g < funcId.size(); g++) {
                                if (funcId.get(g).equals(idGerenteSup) && funcCargo.get(g).equals("GERENTE")) {
                                    gerenciaIndex = g;
                                    break;
                                }
                            }

                            if (gerenciaIndex != -1 && funcSenha.get(gerenciaIndex).equals(senhaGerenteSup)) {
                                System.out.println("[OK] Acesso liberado pela autoridade superior (" +
                                        funcNome.get(gerenciaIndex) + ")!");
                                acessoLiberado = true;
                            } else {
                                System.out.println(">>> ERRO: Credenciais de autoridade invalidas. <<<\n");
                            }
                        }
                    }
                }

                if (!acessoLiberado) continue;

                String cargoAtual = funcCargo.get(funcLogadoIndex);
                System.out.println("\nBem-vindo(a), " + funcNome.get(funcLogadoIndex) + " [" + cargoAtual + "]!");

                boolean loopAdmin = true;
                while (loopAdmin) {
                    System.out.println("\n================================================================");
                    System.out.println("               MENU ADMINISTRATIVO UNIFICADO                    ");
                    System.out.println("================================================================");
                    System.out.println("[1] Gestao de Funcionarios (Cadastrar, Listar, Alterar, Excluir)");
                    System.out.println("[2] Gestao de Fornecedores (Cadastrar, Listar, Alterar, Excluir)");
                    System.out.println("[3] Gestao Financeira, Produtos, Estoque e Central de Pagamentos");
                    System.out.println("[4] Emitir nota fiscal de saida");
                    System.out.println("[5] Alterar minha senha de acesso");
                    System.out.println("[6] Voltar a pagina anterior (Trocar perfil)");
                    System.out.println("[7] Voltar ao inicio do sistema");
                    System.out.println("================================================================");

                    int opcaoMenu = lerInteiroValido(scanner, "Selecione uma opcao (0 para voltar): ");

                    if (opcaoMenu == 6 || opcaoMenu == -999) {
                        break;
                    } else if (opcaoMenu == 7) {
                        loopAdmin = false;
                        continue;
                    }

                    if (opcaoMenu == 1 && !cargoAtual.equals("GERENTE")) {
                        System.out.println("\n[ACESSO NEGADO]: Apenas gerentes gerenciam funcionarios.");
                        continue;
                    }
                    if (opcaoMenu == 3 && cargoAtual.equals("OPERADOR")) {
                        System.out.println("\n[ACESSO NEGADO]: Operadores nao acessam o painel financeiro.");
                        continue;
                    }

                    if (opcaoMenu == 1) {
                        menuGerenciarFuncionarios(scanner, funcId, funcNome, funcCpf, funcCargo, funcSenha, funcSalario, funcPago);
                    } else if (opcaoMenu == 2) {
                        menuGerenciarFornecedores(scanner, fornNome, fornCnpj, fornUf, fornProdutoFornecido,
                                fornPrecoPago, fornLucroPretendidoPct, fornPago);
                    } else if (opcaoMenu == 3) {
                        menuFinanceiroEOperacional(scanner, prodNome, prodNcm, prodPrecos, prodPrecoOriginal, prodEstoque,
                                funcNome, funcCargo, funcSalario, funcPago, fornNome, fornProdutoFornecido, fornPrecoPago, fornPago);
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
                        System.out.println("\n--- PREVIA DA NOTA FISCAL DE SAIDA (Exercicio " + anoExercicioPadrao + ") ---");
                        System.out.println("Valor Bruto Total: " + formatarMoeda(tN));
                        System.out.println("Total de Tributos Calculados (ICMS): " + formatarMoeda(tI));
                    } else if (opcaoMenu == 5) {
                        System.out.print("Digite sua senha atual (0 para voltar): ");
                        String sA = scanner.nextLine().trim();
                        if (sA.equals("0") || sA.equalsIgnoreCase("SAIR")) continue;
                        if (funcSenha.get(funcLogadoIndex).equals(sA)) {
                            System.out.print("Digite a nova senha: ");
                            String nS = scanner.nextLine().trim();
                            if (!nS.isEmpty()) {
                                funcSenha.set(funcLogadoIndex, nS);
                                System.out.println("[OK] Sucesso! Senha alterada.");
                            }
                        } else {
                            System.out.println(">>> ERRO: Senha incorreta. <<<");
                        }
                    }
                }
            }
        }
        System.out.println("\n===============================================================");
        System.out.println("          Obrigado por usar o Protei Creat                      ");
        System.out.println("                  SESSAO ENCERRADA.                             ");
        System.out.println("================================================================");
        scanner.close();
    }
}
