import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class prototo {

    /* =========================================================================
       [MÓDULO 1] TRIBUTÁRIO E DE MATRIZ FISCAL (RECEITA FEDERAL)
       -------------------------------------------------------------------------
       Gerencia as regras de apuração de impostos federais e estaduais, 
       considerando a legislação do ICMS (RICMS/SP), alíquotas padrão de PIS 
       e COFINS, e exceções tributárias vigentes.
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
            return 0.12; // Alíquota interestadual padrão
        }
    }

    public static double buscarAliquotaPis(String ncm) {
        if (ncm.equals("2202.99.00")) return 0.00; // Isenção/Alíquota zero para itens específicos
        return aliquotaPisPadrao;
    }

    public static double buscarAliquotaCofins(String ncm) {
        if (ncm.equals("2202.99.00")) return 0.00;
        return aliquotaCofinsPadrao;
    }

    public static double calcularImpostoItem(double valorBase, double aliquota) {
        return valorBase * aliquota;
    }

    /* =========================================================================
       [MÓDULO 2] SEGURANÇA E UTILITÁRIOS (STRING API)
       -------------------------------------------------------------------------
       Funções auxiliares para geração de senhas temporárias seguras (padrão OWASP)
       e formatação de valores monetários para o padrão brasileiro (R$).
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
       [MÓDULO 3] VALIDAÇÃO RÍGIDA DE TIPOS E DOCUMENTOS
       -------------------------------------------------------------------------
       Garante a integridade das entradas do usuário utilizando expressões regulares
       (Regex), algoritmos matemáticos oficiais (Módulo 11 para CPF/CNPJ) e 
       tratamento preventivo de exceções via try-catch.
       ========================================================================= */
    
    // Valida texto permitindo apenas letras (com suporte a acentuação) e espaços
    public static String lerTextoApenasLetras(Scanner scanner, String mensagem) {
        String entrada = "";
        while (true) {
            try {
                System.out.print(mensagem);
                entrada = scanner.nextLine().trim();

                if (entrada.isEmpty()) {
                    System.out.println("⚠️  [Aviso]: O campo não pode ficar vazio. Por favor, tente novamente.\n");
                } else if (!entrada.matches("^[a-zA-ZáàâãéèêíïóôõúüçÁÀÂÃÉÈÊÍÏÓÔÕÚÜÇ\\s]+$")) {
                    System.out.println("❌ [Erro]: Este campo aceita apenas letras e espaços (números/símbolos são proibidos).\n");
                } else {
                    break;
                }
            } catch (Exception e) {
                System.out.println("❌ [Erro]: Falha na leitura do texto. Tente novamente.\n");
            }
        }
        return entrada.toUpperCase();
    }

    // Valida e sanitiza entradas numéricas inteiras
    public static int lerInteiroValido(Scanner scanner, String mensagem) {
        int valor = 0;
        while (true) {
            try {
                System.out.print(mensagem);
                String entrada = scanner.nextLine().trim();
                String apenasNumeros = entrada.replaceAll("[^0-9]", "");
                
                if (apenasNumeros.isEmpty()) {
                    System.out.println("❌ [Erro]: Digite apenas números inteiros válidos.\n");
                    continue;
                }
                valor = Integer.parseInt(apenasNumeros);
                break;
            } catch (Exception e) {
                System.out.println("❌ [Erro]: Entrada inválida. Digite apenas números.\n");
            }
        }
        return valor;
    }

    // Valida e sanitiza entradas numéricas decimais (aceita vírgula ou ponto)
    public static double lerDoubleValido(Scanner scanner, String mensagem) {
        double valor = 0.0;
        while (true) {
            try {
                System.out.print(mensagem);
                String entrada = scanner.nextLine().trim().replace(",", ".");
                valor = Double.parseDouble(entrada);
                if (valor < 0) {
                    System.out.println("❌ [Erro]: O valor não pode ser negativo.\n");
                    continue;
                }
                break;
            } catch (Exception e) {
                System.out.println("❌ [Erro]: Formato numérico inválido (exemplo correto: 49.90).\n");
            }
        }
        return valor;
    }

    // Validação matemática oficial de CPF (Módulo 11)
    public static String lerCpfValido(Scanner scanner) {
        String cpf = "";
        while (true) {
            try {
                System.out.print("Digite o CPF (Apenas 11 números) [Ex: 80695789554]: ");
                cpf = scanner.nextLine().trim().replaceAll("[^0-9]", "");

                if (cpf.length() != 11 || cpf.matches("^(.)\\1{10}$")) {
                    System.out.println("❌ [Erro]: CPF inválido. Deve conter exatamente 11 dígitos numéricos reais.\n");
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
                System.out.println("❌ [Erro]: Falha ao validar o CPF. Tente novamente.\n");
            }
        }
        return cpf;
    }

    // Validação matemática oficial de CNPJ (Módulo 11 - Receita Federal)
    public static String lerCnpjValido(Scanner scanner) {
        String cnpj = "";
        while (true) {
            try {
                System.out.print("Digite o CNPJ (Apenas 14 números) [Ex: 12345678000190]: ");
                cnpj = scanner.nextLine().trim().replaceAll("[^0-9]", "");

                if (cnpj.length() != 14 || cnpj.matches("^(.)\\1{13}$")) {
                    System.out.println("❌ [Erro]: CNPJ inválido. Deve conter exatamente 14 dígitos numéricos.\n");
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
                System.out.println("❌ [Erro]: Falha ao validar o CNPJ. Tente novamente.\n");
            }
        }
        return cnpj;
    }

    public static String lerCepValido(Scanner scanner) {
        String cep = "";
        while (true) {
            try {
                System.out.print("Digite o CEP (Apenas 8 números) [Ex: 04753060]: ");
                cep = scanner.nextLine().trim().replaceAll("[^0-9]", "");

                if (cep.length() != 8) {
                    System.out.println("❌ [Erro]: CEP inválido. Deve conter exatamente 8 dígitos.\n");
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
        System.out.println("          SISTEMA DE GESTÃO TRIBUTÁVEL E ESTOQUE - L'ESTOQUE    ");
        System.out.println("================================================================");
    }

    /* =========================================================================
       [MÓDULO UNIFICADO] GESTÃO DE FUNCIONÁRIOS (CRUD Completo)
       ========================================================================= */
    public static void menuGerenciarFuncionarios(Scanner scanner, List<String> funcId, List<String> funcNome, List<String> funcCpf, 
                                                 List<String> funcCargo, List<String> funcSenha, List<Double> funcSalario, 
                                                 List<Boolean> funcPago) {
        boolean rodando = true;
        while (rodando) {
            System.out.println("\n----------------------------------------------------------------");
            System.out.println("                   PAINEL: GESTÃO DE FUNCIONÁRIOS               ");
            System.out.println("----------------------------------------------------------------");
            System.out.println("[1] Listar funcionários cadastrados");
            System.out.println("[2] Cadastrar novo funcionário");
            System.out.println("[3] Alterar salário de um funcionário");
            System.out.println("[4] Excluir funcionário");
            System.out.println("[5] Voltar ao menu administrativo anterior");

            int op = lerInteiroValido(scanner, "\nSelecione uma opção: ");

            if (op == 5) {
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
                    System.out.print("ID Funcional Exclusivo [Ex: F04]: "); 
                    String novoId = scanner.nextLine().trim().toUpperCase();

                    if (funcId.contains(novoId)) {
                        System.out.println("❌ [Erro]: Este ID já está em uso no sistema.\n");
                        continue;
                    }

                    String novoNome = lerTextoApenasLetras(scanner, "Nome completo [Ex: Carlos Souza]: ");
                    String novoCpf = lerCpfValido(scanner);

                    if (funcCpf.contains(novoCpf)) {
                        System.out.println("❌ [Erro]: Este CPF já está cadastrado para outro funcionário.\n");
                        continue;
                    }

                    int cOpt = lerInteiroValido(scanner, "Cargo [1 - Gerente | 2 - Operador | 3 - Financeiro]: ");
                    
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
                    System.out.println("🔑 Senha de acesso gerada: " + senhaGerada);
                    System.out.println("💰 Salário padrão atribuído: " + formatarMoeda(salarioPadrao) + "\n");
                } catch (Exception e) {
                    System.out.println("❌ [Erro]: Não foi possível concluir o cadastro.\n");
                }
            } else if (op == 3) {
                int idx = lerInteiroValido(scanner, "Informe o número (índice) do funcionário na lista: ") - 1;
                if (idx >= 0 && idx < funcNome.size()) {
                    double nSal = lerDoubleValido(scanner, "Informe o novo valor do salário (R$): ");
                    funcSalario.set(idx, nSal);
                    System.out.println("✅ Sucesso! Salário atualizado.\n");
                } else {
                    System.out.println("❌ [Erro]: Funcionário não encontrado.\n");
                }
            } else if (op == 4) {
                int idx = lerInteiroValido(scanner, "Informe o número (índice) do funcionário que deseja excluir: ") - 1;
                if (idx >= 0 && idx < funcNome.size()) {
                    funcId.remove(idx);
                    funcNome.remove(idx);
                    funcCpf.remove(idx);
                    funcCargo.remove(idx);
                    funcSenha.remove(idx);
                    funcSalario.remove(idx);
                    funcPago.remove(idx);
                    System.out.println("✅ Sucesso! Funcionário removido do sistema.\n");
                } else {
                    System.out.println("❌ [Erro]: Funcionário não encontrado.\n");
                }
            }
        }
    }

    /* =========================================================================
       [MÓDULO UNIFICADO] GESTÃO DE FORNECEDORES (CRUD Completo)
       ========================================================================= */
    public static void menuGerenciarFornecedores(Scanner scanner, List<String> fornNome, List<String> fornCnpj, List<String> fornUf, 
                                                 List<String> fornProdutoFornecido, List<Double> fornPrecoPago, 
                                                 List<Double> fornLucroPretendidoPct) {
        boolean rodando = true;
        while (rodando) {
            System.out.println("\n----------------------------------------------------------------");
            System.out.println("                   PAINEL: GESTÃO DE FORNECEDORES               ");
            System.out.println("----------------------------------------------------------------");
            System.out.println("[1] Listar fornecedores cadastrados");
            System.out.println("[2] Cadastrar novo fornecedor");
            System.out.println("[3] Alterar preço de custo do fornecedor");
            System.out.println("[4] Excluir fornecedor");
            System.out.println("[5] Voltar ao menu administrativo anterior");

            int op = lerInteiroValido(scanner, "\nSelecione uma opção: ");

            if (op == 5) {
                rodando = false;
            } else if (op == 1) {
                System.out.println("\n================================================================");
                System.out.println("                 LISTA DE FORNECEDORES ATIVOS                   ");
                System.out.println("================================================================");
                for (int i = 0; i < fornNome.size(); i++) {
                    System.out.printf("[%d] Razão Social: %s | CNPJ: %s | Produto: %s | Custo: %s\n",
                            (i + 1), fornNome.get(i), fornCnpj.get(i), fornProdutoFornecido.get(i), formatarMoeda(fornPrecoPago.get(i)));
                }
                System.out.println("================================================================");
            } else if (op == 2) {
                try {
                    String nomeForn = lerTextoApenasLetras(scanner, "Razão Social / Nome [Ex: Suplemax Ltda]: ");
                    String cnpjForn = lerCnpjValido(scanner);

                    if (fornCnpj.contains(cnpjForn)) {
                        System.out.println("❌ [Erro]: Este CNPJ já está cadastrado no sistema.\n");
                        continue;
                    }

                    String uf = lerTextoApenasLetras(scanner, "UF de origem [Ex: SP]: ");
                    System.out.print("Produto fornecido [Ex: Whey Protein]: "); 
                    String prod = scanner.nextLine().trim().toUpperCase();
                    
                    double custo = lerDoubleValido(scanner, "Preço pago ao fornecedor (Custo R$): ");
                    double lucro = lerDoubleValido(scanner, "Lucro pretendido para o mercado (%): ");

                    fornNome.add(nomeForn);
                    fornCnpj.add(cnpjForn);
                    fornUf.add(uf);
                    fornProdutoFornecido.add(prod);
                    fornPrecoPago.add(custo);
                    fornLucroPretendidoPct.add(lucro);

                    System.out.println("✅ Sucesso! Fornecedor cadastrado com sucesso.\n");
                } catch (Exception e) {
                    System.out.println("❌ [Erro]: Falha ao registrar o fornecedor.\n");
                }
            } else if (op == 3) {
                int idx = lerInteiroValido(scanner, "Informe o número (índice) do fornecedor na lista: ") - 1;
                if (idx >= 0 && idx < fornNome.size()) {
                    double nCusto = lerDoubleValido(scanner, "Informe o novo preço de custo (R$): ");
                    fornPrecoPago.set(idx, nCusto);
                    System.out.println("✅ Sucesso! Preço de custo atualizado.\n");
                } else {
                    System.out.println("❌ [Erro]: Fornecedor não encontrado.\n");
                }
            } else if (op == 4) {
                int idx = lerInteiroValido(scanner, "Informe o número (índice) do fornecedor que deseja excluir: ") - 1;
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
       [MÓDULO UNIFICADO] FINANCEIRO, ESTOQUE E TRIBUTOS
       ========================================================================= */
    public static void menuFinanceiroEOperacional(Scanner scanner, List<String> prodNome, List<String> prodNcm, List<Double> prodPrecos, 
                                                  List<Double> prodPrecoOriginal, List<Integer> prodEstoque, 
                                                  double saldoCaixaLoja, double totalGastosEstoque, double totalReceitaVendas, 
                                                  double totalSalariosPagos, int anoExercicioPadrao, String ufOrigem) {
        boolean rodando = true;
        while (rodando) {
            System.out.println("\n----------------------------------------------------------------");
            System.out.println("             PAINEL: FINANCEIRO, ESTOQUE E TRIBUTOS             ");
            System.out.println("----------------------------------------------------------------");
            System.out.println("[1] Cadastrar novo produto e ficha fiscal");
            System.out.println("[2] Ajustar preços de produtos");
            System.out.println("[3] Registrar movimentação de estoque / compras");
            System.out.println("[4] Configurar ano fiscal dos tributos");
            System.out.println("[5] Exibir balanço financeiro e demonstrativo DRE");
            System.out.println("[6] Voltar ao menu administrativo anterior");

            int op = lerInteiroValido(scanner, "\nSelecione uma opção: ");

            if (op == 6) {
                rodando = false;
            } else if (op == 1) {
                try {
                    int idPr = lerInteiroValido(scanner, "Informe o ID/Posição desejada para o produto: ") - 1;

                    if (idPr >= 0) {
                        System.out.print("Nome do produto [Ex: Vitamina C]: ");
                        String nProd = scanner.nextLine().trim().toUpperCase();

                        if (prodNome.contains(nProd)) {
                            System.out.println("❌ [Erro]: Já existe um produto cadastrado com este nome.\n");
                            continue;
                        }

                        double pVenda = lerDoubleValido(scanner, "Preço de venda (R$): ");
                        int eIni = lerInteiroValido(scanner, "Quantidade em estoque inicial: ");
                        System.out.print("Código NCM [Ex: 2106.90.30]: "); 
                        String ncmVal = scanner.nextLine().trim();

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
                        System.out.println("✅ Sucesso! Produto cadastrado e integrado ao estoque.\n");
                    }
                } catch (Exception e) {
                    System.out.println("❌ [Erro]: Falha ao cadastrar o produto.\n");
                }
            } else if (op == 2) {
                if (!prodNome.isEmpty()) {
                    System.out.println("\n================================================================");
                    System.out.println("                  TABELA DE PREÇOS ATUAIS                       ");
                    System.out.println("================================================================");
                    for (int i = 0; i < prodNome.size(); i++) {
                        if (prodNome.get(i) != null) {
                            System.out.println("ID: " + (i + 1) + " | Produto: " + prodNome.get(i) + " | Preço: " + formatarMoeda(prodPrecos.get(i)));
                        }
                    }
                    int idPr = lerInteiroValido(scanner, "\nInforme o ID do produto que deseja alterar: ") - 1;
                    if (idPr >= 0 && idPr < prodNome.size() && prodNome.get(idPr) != null) {
                        double nP = lerDoubleValido(scanner, "Informe o novo preço fixo (R$): ");
                        prodPrecos.set(idPr, nP);
                        System.out.println("✅ Sucesso! Preço alterado.\n");
                    }
                }
            } else if (op == 3) {
                for (int i = 0; i < prodNome.size(); i++) {
                    if (prodNome.get(i) != null) {
                        System.out.println("ID: " + (i + 1) + " | Produto: " + prodNome.get(i) + " | Estoque atual: " + prodEstoque.get(i));
                    }
                }
                int pI = lerInteiroValido(scanner, "\nInforme o ID do produto para reposição: ") - 1;
                if (pI >= 0 && pI < prodNome.size() && prodNome.get(pI) != null) {
                    int qE = lerInteiroValido(scanner, "Quantidade de entrada no estoque: ");
                    double cT = lerDoubleValido(scanner, "Custo total pago pelo lote (R$): ");
                    prodEstoque.set(pI, prodEstoque.get(pI) + qE);
                    System.out.println("✅ Sucesso! Estoque abastecido.\n");
                }
            } else if (op == 4) {
                anoExercicioPadrao = lerInteiroValido(scanner, "Informe o novo ano fiscal de referência [Ex: 2027]: ");
                System.out.println("✅ Sucesso! Ano fiscal alterado para " + anoExercicioPadrao + ".\n");
            } else if (op == 5) {
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

        double saldoCaixaLoja = 100000.00;   
        double totalGastosEstoque = 15000.00; 
        double totalReceitaVendas = 0.0;     
        double totalSalariosPagos = 0.0;     

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

            if (perfilAcesso == 3) {
                sistemaAtivo = false;
                break;
            } else if (perfilAcesso < 1 || perfilAcesso > 2) {
                System.out.println("❌ [Erro]: Opção inválida. Escolha 1, 2 ou 3.\n");
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

                    int opcaoCliente = lerInteiroValido(scanner, "\nSelecione uma opção: ");

                    if (opcaoCliente == 4) {
                        break; 
                    } else if (opcaoCliente == 5) {
                        loopCliente = false;
                        continue; 
                    }

                    if ((opcaoCliente == 2 || opcaoCliente == 3) && !clienteIdentificado) {
                        System.out.println("\n----------------------------------------------------------------");
                        System.out.println("                   IDENTIFICAÇÃO DO CLIENTE                     ");
                        System.out.println("----------------------------------------------------------------");
                        cliNome = lerTextoApenasLetras(scanner, "Nome completo [Ex: Maria Souza]: ");
                        cliCpf = lerCpfValido(scanner);

                        int clienteIdx = cliCpfArr.indexOf(cliCpf);

                        if (clienteIdx != -1) {
                            System.out.println("ℹ️  CPF já cadastrado (" + cliNomeArr.get(clienteIdx) + ").");
                            boolean senhaCorreta = false;
                            int tentativasSenha = 3;

                            while (tentativasSenha > 0 && !senhaCorreta) {
                                System.out.print("Digite sua senha de acesso: ");
                                String senhaDigitada = scanner.nextLine().trim();

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
                                        System.out.println("❌ Limite de tentativas excedido. Retornando ao menu.\n");
                                    }
                                }
                            }
                            if (!senhaCorreta) continue;
                        } else {
                            int optCliNovo = lerInteiroValido(scanner, "CPF não cadastrado. [1 - Cadastrar agora | 2 - Comprar sem cadastro]: ");

                            if (optCliNovo == 1) {
                                String sexoCli = lerTextoApenasLetras(scanner, "Sexo [Ex: M ou F]: ");
                                System.out.print("Data de nascimento [Ex: 10/05/1995]: ");
                                String nascCli = scanner.nextLine().trim();
                                cliUf = lerTextoApenasLetras(scanner, "UF de residência [Ex: SP]: ");
                                cliCep = lerCepValido(scanner);
                                String cliSenha = geradorSenhaAleatoriaSistema();
                                System.out.println("🔑 Senha gerada para próximos acessos: " + cliSenha);

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
                        System.out.println("\n--- ADICIONAR PRODUTO AO CARRINHO ---");
                        boolean temProd = false;
                        for (int i = 0; i < prodNome.size(); i++) {
                            if (prodNome.get(i) != null && prodEstoque.get(i) > 0) {
                                System.out.println("ID: " + (i + 1) + " | " + prodNome.get(i) + " - " + formatarMoeda(prodPrecos.get(i)) + " (Disp: " + prodEstoque.get(i) + ")");
                                temProd = true;
                            }
                        }

                        if (!temProd) {
                            System.out.println("ℹ️  Nenhum produto disponível em estoque no momento.");
                        } else {
                            int posItem = lerInteiroValido(scanner, "\nDigite o ID do produto desejado: ") - 1;

                            if (posItem >= 0 && posItem < prodNome.size() && prodNome.get(posItem) != null && prodEstoque.get(posItem) > 0) {
                                int qtde = lerInteiroValido(scanner, "Digite a quantidade desejada: ");

                                if (qtde > 0 && qtde <= prodEstoque.get(posItem)) {
                                    carrinhoProdIndex.add(posItem);
                                    carrinhoQtde.add(qtde);
                                    System.out.println("✅ Produto adicionado ao carrinho com sucesso!");
                                } else {
                                    System.out.println("❌ [Erro]: Quantidade inválida ou superior ao estoque disponível (" + prodEstoque.get(posItem) + ").");
                                }
                            } else {
                                System.out.println("❌ [Erro]: ID de produto inválido.");
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
                                int acao = lerInteiroValido(scanner, "Escolha uma opção: ");

                                if (acao == 1) {
                                    if (!clienteCadastrado) {
                                        System.out.println("\n--- CADASTRO RÁPIDO PARA FATURAMENTO ---");
                                        String sexoC = lerTextoApenasLetras(scanner, "Sexo [Ex: M]: ");
                                        System.out.print("Data de nascimento [Ex: 10/05/1995]: "); 
                                        String nascC = scanner.nextLine().trim();
                                        cliUf = lerTextoApenasLetras(scanner, "UF de residência [Ex: SP]: ");
                                        cliCep = lerCepValido(scanner);
                                        String senhaGerada = geradorSenhaAleatoriaSistema();

                                        cliNomeArr.add(cliNome);
                                        cliCpfArr.add(cliCpf);
                                        cliSenhaArr.add(senhaGerada);
                                        cliSexoArr.add(sexoC);
                                        cliDataNascArr.add(nascC.isEmpty() ? "01/01/2000" : nascC);
                                        cliUfArr.add(cliUf);
                                        cliCepArr.add(cliCep);
                                        clienteCadastrado = true;
                                        System.out.println("🔑 Cadastro efetivado! Sua senha de acesso é: " + senhaGerada);
                                    }

                                    int pagto = lerInteiroValido(scanner, "\nForma de Pagamento [1 - PIX (10% Desc) | 2 - Crédito à Vista | 3 - Parcelado 3x (+5%)]: ");

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
                                    int idxA = lerInteiroValido(scanner, "Número do item no carrinho para alterar: ") - 1;
                                    if (idxA >= 0 && idxA < carrinhoProdIndex.size()) {
                                        int nQtde = lerInteiroValido(scanner, "Nova quantidade desejada: ");
                                        int idxP = carrinhoProdIndex.get(idxA);
                                        if (nQtde > 0 && nQtde <= prodEstoque.get(idxP)) {
                                            carrinhoQtde.set(idxA, nQtde);
                                            System.out.println("✅ Quantidade atualizada.");
                                        } else {
                                            System.out.println("❌ [Erro]: Estoque insuficiente.");
                                        }
                                    }
                                } else if (acao == 3) {
                                    int idxR = lerInteiroValido(scanner, "Número do item para remover do carrinho: ") - 1;
                                    if (idxR >= 0 && idxR < carrinhoProdIndex.size()) {
                                        carrinhoProdIndex.remove(idxR);
                                        carrinhoQtde.remove(idxR);
                                        System.out.println("✅ Item removido do carrinho.");
                                    }
                                } else {
                                    revisando = false;
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

                int funcLogadoIndex = funcCpf.indexOf(cpfLogin);

                if (funcLogadoIndex == -1) {
                    System.out.println("❌ [Erro]: CPF não cadastrado no sistema. Retornando ao início.\n");
                    continue;
                }

                int tentativas = 3;
                boolean acessoLiberado = false;
                while (tentativas > 0) {
                    System.out.print("Digite sua senha de funcionário: ");
                    String senhaL = scanner.nextLine();
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
                    System.out.println("[3] Gestão Financeira, Produtos, Estoque e Impostos");
                    System.out.println("[4] Pagar folha de salários");
                    System.out.println("[5] Emitir nota fiscal de saída");
                    System.out.println("[6] Alterar minha senha de acesso");
                    System.out.println("[7] Voltar à página anterior (Trocar perfil)");
                    System.out.println("[8] Voltar ao início do sistema");

                    int opcaoMenu = lerInteiroValido(scanner, "\nSelecione uma opção: ");

                    if (opcaoMenu == 7) {
                        break; 
                    } else if (opcaoMenu == 8) {
                        loopAdmin = false;
                        continue; 
                    }

                    // Validação de permissões baseadas no cargo do funcionário
                    if (opcaoMenu == 1 && !cargoAtual.equals("GERENTE")) {
                        System.out.println("\n⛔ [Acesso Negado]: Apenas gerentes podem gerenciar funcionários.");
                        continue;
                    }
                    if ((opcaoMenu == 4) && cargoAtual.equals("OPERADOR")) {
                        System.out.println("\n⛔ [Acesso Negado]: Operadores não possuem permissão para pagar folha.");
                        continue;
                    }

                    // Chamada dos submódulos
                    if (opcaoMenu == 1) {
                        menuGerenciarFuncionarios(scanner, funcId, funcNome, funcCpf, funcCargo, funcSenha, funcSalario, funcPago);
                    } else if (opcaoMenu == 2) {
                        menuGerenciarFornecedores(scanner, fornNome, fornCnpj, fornUf, fornProdutoFornecido, fornPrecoPago, fornLucroPretendidoPct);
                    } else if (opcaoMenu == 3) {
                        menuFinanceiroEOperacional(scanner, prodNome, prodNcm, prodPrecos, prodPrecoOriginal, prodEstoque, 
                                                   saldoCaixaLoja, totalGastosEstoque, totalReceitaVendas, totalSalariosPagos, 
                                                   anoExercicioPadrao, ufOrigem);
                    } else if (opcaoMenu == 4) {
                        double folhaTot = 0.0;
                        for (int f = 0; f < funcNome.size(); f++) {
                            if (!funcPago.get(f)) folhaTot += funcSalario.get(f);
                        }
                        System.out.println("Total pendente de folha salarial: " + formatarMoeda(folhaTot));
                        if (folhaTot > 0) {
                            System.out.print("Deseja quitar a folha agora? [S/N]: ");
                            if (scanner.next().equalsIgnoreCase("S")) {
                                if (saldoCaixaLoja >= folhaTot) {
                                    saldoCaixaLoja -= folhaTot;
                                    totalSalariosPagos += folhaTot;
                                    for (int f = 0; f < funcPago.size(); f++) {
                                        funcPago.set(f, true);
                                    }
                                    System.out.println("✅ Sucesso! Folha salarial quitada.");
                                } else {
                                    System.out.println("❌ [Erro]: Saldo em caixa insuficiente para quitar os salários.");
                                }
                            }
                        }
                    } else if (opcaoMenu == 5) {
                        String ufD = lerTextoApenasLetras(scanner, "Informe a UF de destino [Ex: RJ]: ");
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
                    } else if (opcaoMenu == 6) {
                        System.out.print("Digite sua senha atual: ");
                        String sA = scanner.nextLine();
                        if (funcSenha.get(funcLogadoIndex).equals(sA)) {
                            System.out.print("Digite a nova senha: ");
                            String nS = scanner.nextLine().trim();
                            if (!nS.isEmpty()) {
                                funcSenha.set(funcLogadoIndex, nS);
                                System.out.println("✅ Sucesso! Senha alterada.");
                            }
                        } else {
                            System.out.println("❌ [Erro]: Senha atual incorreta.");
                        }
                    }
                }
            }
        }
        System.out.println("\n================================================================");
        System.out.println("         SESSÃO ENCERRADA. OBRIGADO POR USAR O L'ESTOQUE.       ");
        System.out.println("================================================================");
        scanner.close();
    }
}