import java.util.InputMismatchException;
import java.util.Scanner;

public class SistemaTributavelCompleto {

    /* =========================================================================
       MÓDULO TRIBUTÁRIO E DE MATRIZ FISCAL (LUCRO REAL / NCM / CEST / CFOP / CST)
       ========================================================================= */

    public static double buscarAliquotaIcms(String ncm, String ufOrigem, String ufDestino) {
        if (ufOrigem.equalsIgnoreCase("SP") && ufDestino.equalsIgnoreCase("SP")) {
            return 0.18; // 18% ICMS interno SP
        } else {
            return 0.12; // Alíquota interestadual genérica
        }
    }

    public static double buscarAliquotaPis(String ncm) {
        if (ncm.equals("2202.99.00")) {
            return 0.00; // Tributação Monofásica (Bebidas Isotônicas)
        } else {
            return 0.0165; // 1.65% Lucro Real Padrão
        }
    }

    public static double buscarAliquotaCofins(String ncm) {
        if (ncm.equals("2202.99.00")) {
            return 0.00; // Tributação Monofásica (Bebidas Isotônicas)
        } else {
            return 0.0760; // 7.60% Lucro Real Padrão
        }
    }

    public static double calcularImpostoItem(double valorBase, double aliquota) {
        return valorBase * aliquota;
    }

    /* =========================================================================
       MÓDULO DE VALIDAÇÃO DE ENTRADA (CPF)
       ========================================================================= */

    public static String lerCpfValido(Scanner scanner) {
        String cpf = "";
        while (true) {
            System.out.print("CPF (DIGITE APENAS OS 11 NÚMEROS, SEM PONTOS OU HÍFEN): ");
            cpf = scanner.nextLine().trim().replaceAll("[^0-9]", "");

            if (cpf.length() > 11) {
                System.out.println(">>> ERRO: O CPF DEVE TER NO MÁXIMO 11 DÍGITOS! VOCÊ DIGITOU " 
                                   + cpf.length() + " CARACTERES. TENTE NOVAMENTE. <<<");
            } else if (cpf.length() < 11) {
                System.out.println(">>> AVISO: CPF DEVE CONTER EXATAMENTE 11 DÍGITOS. DIGITE NOVAMENTE. <<<");
            } else {
                break;
            }
        }
        return cpf;
    }

    /* =========================================================================
       MÓDULO DE DUPLA VERIFICAÇÃO E MOVIMENTAÇÃO DE ESTOQUE
       ========================================================================= */

    public static boolean confirmarDuplaVerificacao(Scanner scanner, int qtdAtual, int qtdAlteracao, String tipoOperacao) {
        System.out.println("\n--------------------------------------------------");
        System.out.println(" [DUPLA VERIFICAÇÃO] CONFIRMAÇÃO DE SEGURANÇA");
        System.out.println(" TIPO OPERAÇÃO: " + tipoOperacao);
        System.out.println(" QUANTIDADE ATUAL EM ESTOQUE: " + qtdAtual);
        System.out.println(" QUANTIDADE A MOVER/AJUSTAR: " + qtdAlteracao);
        System.out.print(" CONFIRMA A OPERAÇÃO? (S/N): ");
        String confirmacao = scanner.next();

        if (confirmacao.equalsIgnoreCase("S")) {
            return true;
        } else {
            System.out.println(">>> OPERAÇÃO CANCELADA PELO USUÁRIO! <<<");
            return false;
        }
    }

    public static void exibirCabecalho() {
        System.out.println("==================================================");
        System.out.println("     SISTEMA DE GESTÃO TRIBUTÁVEL E ESTOQUE       ");
        System.out.println("==================================================");
    }

    /* =========================================================================
       MÉTODO PRINCIPAL
       ========================================================================= */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Estrutura de Cadastro de Funcionários e Hierarquia (Até 10 funcionários)
        String[] funcNome = new String[10];
        String[] funcCpf = new String[10];
        String[] funcCargo = new String[10]; // "GERENTE" ou "OPERADOR"
        int totalFuncionarios = 0;

        // Estrutura do Cliente
        String cliNome = "", cliCpf = "", cliSexo = "", cliDataNasc = "", cliUf = "";

        // Estruturas de Dados para Produtos (Até 10 itens)
        String[] prodNome = new String[10];
        String[] prodNcm = new String[10];
        String[] prodCest = new String[10];
        String[] prodCfop = new String[10];
        String[] prodCst = new String[10];
        double[] prodPrecos = new double[10];
        int[] prodEstoque = new int[10];
        int[] prodCategoria = new int[10];
        int totalProdutos = 0;

        // Estrutura do Carrinho de Compras (Até 10 itens acumulados)
        int[] carrinhoProdIndex = new int[10];
        int[] carrinhoQtde = new int[10];
        int totalItensCarrinho = 0;

        // Fornecedores (Até 5)
        String[] fornNome = new String[5];
        String[] fornCnpj = new String[5];
        String[] fornUf = new String[5];
        int totalFornecedores = 0;

        // Histórico de Movimentações (Até 20)
        String[] histTipo = new String[20];
        String[] histUsuario = new String[20];
        String[] histProduto = new String[20];
        int[] histQtde = new int[20];
        int totalHistorico = 0;

        String ufOrigem = "SP";
        int perfilAcesso = 0;

        exibirCabecalho();

        System.out.println("IDENTIFIQUE SEU PERFIL DE ACESSO:");
        System.out.println("1 - CLIENTE (Realizar Compras / Acumular no Carrinho)");
        System.out.println("2 - FUNCIONÁRIO (Acesso ao Painel Administrativo)");
        System.out.print("ESCOLHA UMA OPÇÃO: ");
        
        try {
            perfilAcesso = scanner.nextInt();
            scanner.nextLine();
        } catch (InputMismatchException e) {
            System.out.println(">>> ERRO: OPÇÃO INVÁLIDA! ENCERRANDO SISTEMA. <<<");
            scanner.close();
            return;
        }

        /* =========================================================================
           FLUXO DE ACESSO DO CLIENTE COM CARRINHO DE COMPRAS
           ========================================================================= */
        if (perfilAcesso == 1) {
            System.out.println("\n--- CADASTRO OBRIGATÓRIO DE CLIENTE ---");
            System.out.print("NOME COMPLETO: ");
            cliNome = scanner.nextLine();
            
            cliCpf = lerCpfValido(scanner);

            System.out.print("SEXO (M/F/Outro): ");
            cliSexo = scanner.nextLine();
            System.out.print("DATA DE NASCIMENTO (DD/MM/AAAA): ");
            cliDataNasc = scanner.nextLine();
            System.out.print("UF DE RESIDÊNCIA (Ex: SP, RJ, MG): ");
            cliUf = scanner.nextLine().toUpperCase();

            System.out.println("\n>>> BEM-VINDO(A), " + cliNome.toUpperCase() + "! <<<");

            // Produtos base pré-cadastrados para demonstrar a navegação e carrinho
            prodNome[0] = "WHEY PROTEIN ISOLADO";
            prodCategoria[0] = 1;
            prodPrecos[0] = 120.00;
            prodEstoque[0] = 50;
            prodNcm[0] = "2106.90.30";
            prodCest[0] = "NULO";
            prodCfop[0] = "5102";
            prodCst[0] = "00";

            prodNome[1] = "BEBIDA ISOTÔNICA 500ML";
            prodCategoria[1] = 2;
            prodPrecos[1] = 8.50;
            prodEstoque[1] = 100;
            prodNcm[1] = "2202.99.00";
            prodCest[1] = "NULO";
            prodCfop[1] = "5102";
            prodCst[1] = "60";

            totalProdutos = 2;

            int opcaoCliente = 0;
            while (opcaoCliente != 4) {
                System.out.println("\n=================== PAINEL DO CLIENTE ===================");
                System.out.println("1 - VISUALIZAR CATÁLOGO DE PRODUTOS");
                System.out.println("2 - ADICIONAR PRODUTO AO CARRINHO DE COMPRAS");
                System.out.println("3 - VISUALIZAR CARRINHO E FINALIZAR COMPRA (GERAR NOTA FISCAL)");
                System.out.println("4 - SAIR DO SISTEMA");
                System.out.print("ESCOLHA UMA OPÇÃO: ");

                try {
                    opcaoCliente = scanner.nextInt();
                    scanner.nextLine();
                } catch (InputMismatchException e) {
                    System.out.println(">>> ERRO: DIGITE APENAS NÚMEROS VÁLIDOS! <<<");
                    scanner.nextLine();
                    continue;
                }

                if (opcaoCliente == 1) {
                    System.out.println("\n--- CATÁLOGO DE PRODUTOS DISPONÍVEIS ---");
                    for (int i = 0; i < totalProdutos; i++) {
                        System.out.println("ID: " + (i + 1) + " | PRODUTO: " + prodNome[i] + 
                                " | PREÇO: R$ " + String.format("%.2f", prodPrecos[i]) + 
                                " | ESTOQUE DISPONÍVEL: " + prodEstoque[i]);
                    }

                } else if (opcaoCliente == 2) {
                    System.out.println("\n--- ADICIONAR PRODUTOS AO CARRINHO ---");
                    for (int i = 0; i < totalProdutos; i++) {
                        System.out.println("ID: " + (i + 1) + " | PRODUTO: " + prodNome[i] + 
                                " | PREÇO: R$ " + String.format("%.2f", prodPrecos[i]) + 
                                " | ESTOQUE: " + prodEstoque[i]);
                    }
                    
                    System.out.print("DIGITE O ID DO PRODUTO QUE DESEJA ADICIONAR: ");
                    int posItem = scanner.nextInt() - 1;

                    if (posItem >= 0 && posItem < totalProdutos) {
                        System.out.print("DIGITE A QUANTIDADE DESEJADA: ");
                        int qtdeComprar = scanner.nextInt();

                        if (qtdeComprar <= prodEstoque[posItem]) {
                            if (totalItensCarrinho < 10) {
                                carrinhoProdIndex[totalItensCarrinho] = posItem;
                                carrinhoQtde[totalItensCarrinho] = qtdeComprar;
                                totalItensCarrinho++;
                                
                                System.out.println(">>> PRODUTO '" + prodNome[posItem] + "' (QTDE: " + qtdeComprar + ") ADICIONADO AO CARRINHO COM SUCESSO! <<<");
                            } else {
                                System.out.println(">>> O SEU CARRINHO JÁ ATINGIU O LIMITE MÁXIMO DE ITENS. <<<");
                            }
                        } else {
                            System.out.println(">>> ESTOQUE INSUFICIENTE PARA ESSA QUANTIDADE! <<<");
                        }
                    } else {
                        System.out.println("ID DE PRODUTO INVÁLIDO!");
                    }

                } else if (opcaoCliente == 3) {
                    if (totalItensCarrinho == 0) {
                        System.out.println("SEU CARRINHO ESTÁ VAZIO. ADICIONE ITENS ANTES DE FINALIZAR A COMPRA.");
                    } else {
                        double valorTotalCarrinho = 0.0;
                        double totalIcms = 0.0, totalPis = 0.0, totalCofins = 0.0;

                        System.out.println("\n==================================================");
                        System.out.println("             CARRINHO DE COMPRAS ACUMULADO        ");
                        System.out.println("==================================================");
                        
                        for (int i = 0; i < totalItensCarrinho; i++) {
                            int idxP = carrinhoProdIndex[i];
                            double valorTotalItem = prodPrecos[idxP] * carrinhoQtde[i];
                            valorTotalCarrinho += valorTotalItem;
                            
                            System.out.printf("ITEM %d: %s | QTDE: %d | VAL. UNIT: R$ %.2f | TOTAL: R$ %.2f\n", 
                                    (i + 1), prodNome[idxP], carrinhoQtde[i], prodPrecos[idxP], valorTotalItem);
                        }
                        
                        System.out.println("--------------------------------------------------");
                        System.out.printf("VALOR TOTAL ACUMULADO DO CARRINHO: R$ %.2f\n", valorTotalCarrinho);
                        
                        System.out.print("DESEJA FINALIZAR A COMPRA E EMITIR A NOTA FISCAL? (S/N): ");
                        String confirmarCompra = scanner.next();

                        if (confirmarCompra.equalsIgnoreCase("S")) {
                            System.out.println("\n==================================================");
                            System.out.println("          NOTA FISCAL ELETRÔNICA DE COMPRA        ");
                            System.out.println(" CLIENTE: " + cliNome.toUpperCase() + " | CPF: " + cliCpf);
                            System.out.println(" DATA NASC.: " + cliDataNasc + " | SEXO: " + cliSexo);
                            System.out.println(" UF ORIGEM: " + ufOrigem + " | UF DESTINO: " + cliUf);
                            System.out.println("==================================================");

                            // Processa todos os itens do carrinho de uma só vez
                            for (int i = 0; i < totalItensCarrinho; i++) {
                                int idxP = carrinhoProdIndex[i];
                                double valorTotalItem = prodPrecos[idxP] * carrinhoQtde[i];

                                double aliqIcms = buscarAliquotaIcms(prodNcm[idxP], ufOrigem, cliUf);
                                double aliqPis = buscarAliquotaPis(prodNcm[idxP]);
                                double aliqCofins = buscarAliquotaCofins(prodNcm[idxP]);

                                double valIcms = calcularImpostoItem(valorTotalItem, aliqIcms);
                                double valPis = calcularImpostoItem(valorTotalItem, aliqPis);
                                double valCofins = calcularImpostoItem(valorTotalItem, aliqCofins);

                                totalIcms += valIcms;
                                totalPis += valPis;
                                totalCofins += valCofins;

                                // Abate do estoque do produto
                                prodEstoque[idxP] -= carrinhoQtde[i];

                                // Registro no histórico
                                if (totalHistorico < 20) {
                                    histTipo[totalHistorico] = "VENDA CLIENTE";
                                    histProduto[totalHistorico] = prodNome[idxP];
                                    histQtde[totalHistorico] = carrinhoQtde[i];
                                    histUsuario[totalHistorico] = "CLIENTE: " + cliNome;
                                    totalHistorico++;
                                }

                                System.out.println("ITEM " + (i + 1) + ": " + prodNome[idxP] + " | QUANTIDADE: " + carrinhoQtde[i]);
                                System.out.println("   ↳ DADOS FISCAIS -> NCM: " + prodNcm[idxP] + " | CFOP: " + prodCfop[idxP]);
                                System.out.printf("   ↳ VALOR ITEM BRUTO: R$ %.2f\n", valorTotalItem);
                                System.out.printf("   ↳ TRIBUTOS -> ICMS: R$ %.2f | PIS: R$ %.2f | COFINS: R$ %.2f\n", valIcms, valPis, valCofins);
                                System.out.println("--------------------------------------------------");
                            }

                            System.out.println("====== RESUMO FINAL DA COMPRA E TRIBUTOS ======");
                            System.out.printf("VALOR TOTAL DA COMPRA: R$ %.2f\n", valorTotalCarrinho);
                            System.out.printf("TOTAL ICMS RECOLHIDO: R$ %.2f\n", totalIcms);
                            System.out.printf("TOTAL PIS RECOLHIDO: R$ %.2f\n", totalPis);
                            System.out.printf("TOTAL COFINS RECOLHIDO: R$ %.2f\n", totalCofins);
                            System.out.printf("TOTAL DE TRIBUTOS INCIDENTES: R$ %.2f\n", (totalIcms + totalPis + totalCofins));
                            System.out.println("==================================================");
                            System.out.println(">>> COMPRA FINALIZADA COM SUCESSO! OBRIGADO! <<<");

                            // Esvazia o carrinho
                            totalItensCarrinho = 0;
                        } else {
                            System.out.println(">>> COMPRA MANTIDA EM ABERTO NO CARRINHO. <<<");
                        }
                    }
                }
            }

        /* =========================================================================
           FLUXO DE ACESSO DO FUNCIONÁRIO / HIERARQUIA
           ========================================================================= */
        } else if (perfilAcesso == 2) {
            System.out.println("\n--- IDENTIFICAÇÃO DO FUNCIONÁRIO ---");
            
            if (totalFuncionarios == 0) {
                System.out.println("NENHUM FUNCIONÁRIO CADASTRADO. REALIZANDO CADASTRO DO GERENTE TITULAR:");
                System.out.print("NOME COMPLETO: ");
                funcNome[0] = scanner.nextLine();
                
                funcCpf[0] = lerCpfValido(scanner);
                funcCargo[0] = "GERENTE";
                totalFuncionarios = 1;
                System.out.println(">>> GERENTE TITULAR CADASTRADO COM SUCESSO! <<<");
            }

            int funcLogadoIndex = 0;

            System.out.println("\n>>> PAINEL OPERACIONAL INICIADO POR: " + funcNome[funcLogadoIndex].toUpperCase() 
                               + " [" + funcCargo[funcLogadoIndex] + "] <<<");

            int opcaoMenu = 0;
            while (opcaoMenu != 7) {
                System.out.println("\n=================== MENU ADMINISTRATIVO ===================");
                System.out.println("1 - CADASTRAR NOVO FUNCIONÁRIO (RESTRITO AO GERENTE)");
                System.out.println("2 - CADASTRAR FORNECEDORES");
                System.out.println("3 - CADASTRAR PRODUTO E INFORMAÇÕES FISCAIS (NCM/CEST)");
                System.out.println("4 - MOVIMENTAÇÃO DE ESTOQUE (ENTRADA / SAÍDA / AJUSTE / DEVOLUÇÃO)");
                System.out.println("5 - EMITIR NOTA FISCAL DE VENDA (CÁLCULO AUTOMÁTICO DE IMPOSTOS)");
                System.out.println("6 - HISTÓRICO DE MOVIMENTAÇÕES DE ESTOQUE");
                System.out.println("7 - SAIR DO SISTEMA");
                System.out.print("ESCOLHA UMA OPÇÃO: ");

                try {
                    opcaoMenu = scanner.nextInt();
                    scanner.nextLine();
                } catch (InputMismatchException e) {
                    System.out.println(">>> ERRO: DIGITE APENAS NÚMEROS NO MENU! <<<");
                    scanner.nextLine();
                    continue;
                }

                if (opcaoMenu == 1) {
                    if (!funcCargo[funcLogadoIndex].equals("GERENTE")) {
                        System.out.println("\n>>> ACESSO NEGADO! APENAS FUNCIONÁRIOS COM HIERARQUIA DE GERENTE PODEM CADASTRAR OUTROS FUNCIONÁRIOS. <<<");
                    } else {
                        if (totalFuncionarios < 10) {
                            System.out.println("\n--- CADASTRO DE NOVO FUNCIONÁRIO ---");
                            System.out.print("NOME COMPLETO DO NOVO FUNCIONÁRIO: ");
                            funcNome[totalFuncionarios] = scanner.nextLine();

                            funcCpf[totalFuncionarios] = lerCpfValido(scanner);

                            System.out.println("SELEÇÃO DE HIERARQUIA:");
                            System.out.println("1 - GERENTE");
                            System.out.println("2 - OPERADOR");
                            System.out.print("ESCOLHA O CARGO DO FUNCIONÁRIO: ");
                            int opcaoCargo = scanner.nextInt();
                            scanner.nextLine();

                            if (opcaoCargo == 1) {
                                funcCargo[totalFuncionarios] = "GERENTE";
                            } else {
                                funcCargo[totalFuncionarios] = "OPERADOR";
                            }

                            totalFuncionarios++;
                            System.out.println(">>> NOVO FUNCIONÁRIO CADASTRADO COM SUCESSO! <<<");
                        } else {
                            System.out.println("LIMITE MÁXIMO DE FUNCIONÁRIOS ATINGIDO.");
                        }
                    }

                } else if (opcaoMenu == 2) {
                    if (totalFornecedores < 5) {
                        System.out.println("\n--- CADASTRO DE FORNECEDOR " + (totalFornecedores + 1) + " ---");
                        System.out.print("NOME / RAZÃO SOCIAL: ");
                        fornNome[totalFornecedores] = scanner.nextLine();

                        System.out.print("CNPJ: ");
                        fornCnpj[totalFornecedores] = scanner.nextLine();

                        System.out.print("UF DO FORNECEDOR (Ex: SP, RJ, MG): ");
                        fornUf[totalFornecedores] = scanner.nextLine().toUpperCase();

                        totalFornecedores++;
                        System.out.println(">>> FORNECEDOR CADASTRADO COM SUCESSO! <<<");
                    } else {
                        System.out.println("LIMITE MÁXIMO DE FORNECEDORES ATINGIDO.");
                    }

                } else if (opcaoMenu == 3) {
                    if (totalProdutos < 10) {
                        int i = totalProdutos;
                        System.out.println("\n--- CADASTRO DO PRODUTO " + (i + 1) + " ---");
                        System.out.print("NOME DO PRODUTO: ");
                        prodNome[i] = scanner.nextLine();

                        System.out.print("CATEGORIA (1 - Suplemento | 2 - Bebida Isotônica | 3 - Vestuário | 4 - Calçado): ");
                        prodCategoria[i] = scanner.nextInt();

                        System.out.print("PREÇO UNITÁRIO: R$ ");
                        prodPrecos[i] = scanner.nextDouble();

                        System.out.print("ESTOQUE INICIAL: ");
                        prodEstoque[i] = scanner.nextInt();
                        scanner.nextLine();

                        System.out.println("--- FICHA FISCAL E TRIBUTÁRIA ---");
                        System.out.print("NCM (Ex: 2106.90.30 / 2202.99.00 / 6109.90.00 / 6404.11.00): ");
                        prodNcm[i] = scanner.nextLine();

                        System.out.print("CEST (Código Especificador da Substituição Tributária): ");
                        prodCest[i] = scanner.nextLine();

                        System.out.print("CFOP REGISTRADO (Ex: 5102 - Venda de mercadoria): ");
                        prodCfop[i] = scanner.nextLine();

                        System.out.print("CST/CSOSN (Ex: 00 - Tributada integralmente / 60 - ICMS cobrado ant.): ");
                        prodCst[i] = scanner.nextLine();

                        totalProdutos++;
                        System.out.println(">>> PRODUTO E FICHA FISCAL CADASTRADOS COM SUCESSO! <<<");
                    } else {
                        System.out.println("LIMITE MÁXIMO DE PRODUTOS ATINGIDO.");
                    }

                } else if (opcaoMenu == 4) {
                    if (totalProdutos == 0) {
                        System.out.println("NENHUM PRODUTO CADASTRADO.");
                    } else {
                        System.out.println("\n--- SELEÇÃO DE PRODUTO PARA MOVIMENTAÇÃO ---");
                        for (int i = 0; i < totalProdutos; i++) {
                            System.out.println("ID: " + (i + 1) + " | PRODUTO: " + prodNome[i] + " | ESTOQUE ATUAL: " + prodEstoque[i]);
                        }

                        System.out.print("DIGITE O ID DO PRODUTO: ");
                        int posItem = scanner.nextInt() - 1;

                        if (posItem >= 0 && posItem < totalProdutos) {
                            System.out.print("TIPO DE OPERAÇÃO (ENTRADA / SAIDA / AJUSTE / DEVOLUCAO): ");
                            String tipoMov = scanner.next().toUpperCase();

                            System.out.print("QUANTIDADE A MOVER/AJUSTAR (DIGITE APENAS NÚMEROS): ");
                            try {
                                int qtdeMover = scanner.nextInt();

                                boolean confirmado = confirmarDuplaVerificacao(scanner, prodEstoque[posItem], qtdeMover, tipoMov);

                                if (confirmado) {
                                    if (tipoMov.equals("ENTRADA") || tipoMov.equals("DEVOLUCAO")) {
                                        prodEstoque[posItem] += qtdeMover;
                                    } else if (tipoMov.equals("SAIDA")) {
                                        prodEstoque[posItem] -= qtdeMover;
                                    } else if (tipoMov.equals("AJUSTE")) {
                                        prodEstoque[posItem] = qtdeMover;
                                    }

                                    if (totalHistorico < 20) {
                                        histTipo[totalHistorico] = tipoMov;
                                        histProduto[totalHistorico] = prodNome[posItem];
                                        histQtde[totalHistorico] = qtdeMover;
                                        histUsuario[totalHistorico] = "FUNC (" + funcCargo[funcLogadoIndex] + "): " + funcNome[funcLogadoIndex];
                                        totalHistorico++;
                                    }

                                    System.out.println(">>> MOVIMENTAÇÃO REALIZADA! NOVO ESTOQUE: " + prodEstoque[posItem] + " <<<");
                                }
                            } catch (InputMismatchException e) {
                                System.out.println(">>> ERRO: A QUANTIDADE DEVE SER UM NÚMERO INTEIRO! <<<");
                                scanner.nextLine();
                            }
                        } else {
                            System.out.println("ID DE PRODUTO INVÁLIDO!");
                        }
                    }

                } else if (opcaoMenu == 5) {
                    if (totalProdutos == 0) {
                        System.out.println("NENHUM PRODUTO CADASTRADO PARA EMISSÃO DE NOTA FISCAL.");
                    } else {
                        System.out.println("\n--- DADOS DA OPERAÇÃO DA NOTA FISCAL DE SAÍDA ---");
                        System.out.print("UF DE DESTINO DA VENDA (Ex: SP, RJ, MG): ");
                        String ufDestino = scanner.next().toUpperCase();

                        double valorTotalNota = 0.0;
                        double totalIcms = 0.0, totalPis = 0.0, totalCofins = 0.0;

                        System.out.println("\n==================================================");
                        System.out.println("          NOTA FISCAL ELETRÔNICA DE SAÍDA         ");
                        System.out.println(" RESPONSÁVEL: " + funcNome[funcLogadoIndex].toUpperCase() + " | CPF: " + funcCpf[funcLogadoIndex]);
                        System.out.println(" CARGO: " + funcCargo[funcLogadoIndex]);
                        System.out.println(" UF ORIGEM: " + ufOrigem + " | UF DESTINO: " + ufDestino);
                        System.out.println("==================================================");

                        for (int i = 0; i < totalProdutos; i++) {
                            double valorItem = prodPrecos[i] * prodEstoque[i];
                            valorTotalNota += valorItem;

                            double aliqIcms = buscarAliquotaIcms(prodNcm[i], ufOrigem, ufDestino);
                            double aliqPis = buscarAliquotaPis(prodNcm[i]);
                            double aliqCofins = buscarAliquotaCofins(prodNcm[i]);

                            double valIcms = calcularImpostoItem(valorItem, aliqIcms);
                            double valPis = calcularImpostoItem(valorItem, aliqPis);
                            double valCofins = calcularImpostoItem(valorItem, aliqCofins);

                            totalIcms += valIcms;
                            totalPis += valPis;
                            totalCofins += valCofins;

                            if (totalHistorico < 20) {
                                histTipo[totalHistorico] = "EMISSÃO NF";
                                histProduto[totalHistorico] = prodNome[i];
                                histQtde[totalHistorico] = prodEstoque[i];
                                histUsuario[totalHistorico] = "FUNC: " + funcNome[funcLogadoIndex];
                                totalHistorico++;
                            }

                            System.out.println("ITEM " + (i + 1) + ": " + prodNome[i]);
                            System.out.println("   ↳ DADOS FISCAIS -> NCM: " + prodNcm[i] + " | CEST: " + prodCest[i] + " | CFOP: " + prodCfop[i] + " | CST: " + prodCst[i]);
                            System.out.printf("   ↳ VALOR ITEM: R$ %.2f\n", valorItem);
                            System.out.printf("   ↳ TRIBUTOS -> ICMS: R$ %.2f | PIS: R$ %.2f | COFINS: R$ %.2f\n", valIcms, valPis, valCofins);
                            System.out.println("--------------------------------------------------");
                        }

                        System.out.println("====== RESUMO DE TRIBUTOS E VALORES DA NOTA ======");
                        System.out.printf("VALOR TOTAL BRUTO DA NOTA: R$ %.2f\n", valorTotalNota);
                        System.out.printf("TOTAL ICMS RECOLHIDO: R$ %.2f\n", totalIcms);
                        System.out.printf("TOTAL PIS RECOLHIDO: R$ %.2f\n", totalPis);
                        System.out.printf("TOTAL COFINS RECOLHIDO: R$ %.2f\n", totalCofins);
                        System.out.printf("TOTAL DE TRIBUTOS INCIDENTES: R$ %.2f\n", (totalIcms + totalPis + totalCofins));
                        System.out.println("==================================================");
                    }

                } else if (opcaoMenu == 6) {
                    System.out.println("\n--- HISTÓRICO DE MOVIMENTAÇÕES DE ESTOQUE ---");
                    if (totalHistorico == 0) {
                        System.out.println("NENHUMA MOVIMENTAÇÃO REGISTRADA ATÉ O MOMENTO.");
                    } else {
                        for (int i = 0; i < totalHistorico; i++) {
                            System.out.println("REGISTRO " + (i + 1) + " | OPERAÇÃO: " + histTipo[i] + 
                                    " | PRODUTO: " + histProduto[i] + 
                                    " | QTDE: " + histQtde[i] + 
                                    " | REGISTRADO POR: " + histUsuario[i]);
                        }
                    }
                }
            }
        } else {
            System.out.println("PERFIL DE ACESSO INVÁLIDO!");
        }

        System.out.println("\n>>> SESSÃO FINALIZADA. ATÉ LOGO! <<<");
        scanner.close();
    }
}
