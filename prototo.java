import java.util.InputMismatchException;
import java.util.Scanner;
import java.util.Random;

public class prototo {

    /* =========================================================================
       [MÓDULO 1] TRIBUTÁRIO E DE MATRIZ FISCAL (RECEITA FEDERAL / ANOS 2026 E 2027)
       -------------------------------------------------------------------------
       FONTE DAS INFORMAÇÕES: 
       - Legislação tributária brasileira para o ICMS (Regulamento do ICMS - RICMS/SP).
       - Diretrizes federais de transição da Reforma Tributária para PIS (1,65%) 
         e COFINS (7,60%), com alíquota zero aplicada a produtos específicos (ex: NCM 2202.99.00).
       - Regras de alíquota interestadual padrão (12%) vs interna (18% em 2026 e 19% em 2027).
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

    public static double buscarAliquotaPis(String ncm) {
        if (ncm.equals("2202.99.00")) {
            return 0.00; 
        } else {
            return aliquotaPisPadrao; 
        }
    }

    public static double buscarAliquotaCofins(String ncm) {
        if (ncm.equals("2202.99.00")) {
            return 0.00; 
        } else {
            return aliquotaCofinsPadrao; 
        }
    }

    public static double calcularImpostoItem(double valorBase, double aliquota) {
        return valorBase * aliquota;
    }

    /* =========================================================================
       [MÓDULO 2] SEGURANÇA, CREDENCIAIS E FORMATAÇÃO UTILITÁRIA
       -------------------------------------------------------------------------
       FONTE DAS INFORMAÇÕES: 
       - Padrões de segurança da informação (OWASP) para senhas temporárias.
       - Padrões de formatação monetária corporativa brasileira.
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
       [MÓDULO 3] VALIDAÇÃO ROBUSTA DE ENTRADAS DE DADOS
       -------------------------------------------------------------------------
       FONTE DAS INFORMAÇÕES: 
       - Algoritmo oficial da Receita Federal do Brasil para validação matemática 
         dos Dígitos Verificadores (DV) do CPF (módulo 11).
       - Padrões de formatação de CEP do sistema de endereçamento postal dos Correios (8 dígitos).
       ========================================================================= */
    public static String lerNomeValido(Scanner scanner, String mensagemPersonalizada) {
        String nome = "";
        while (true) {
            System.out.print(mensagemPersonalizada);
            nome = scanner.nextLine().trim();

            if (nome.isEmpty()) {
                System.out.println(">>> ERRO: O campo não pode ficar vazio!");
                System.out.println(">>> O que é permitido: Apenas letras e espaços.");
                System.out.println(">>> Exemplo correto: MARIO SILVA. Tente novamente. <<<\n");
            } else if (nome.matches(".*\\d.*")) {
                System.out.println(">>> ERRO: O nome não pode conter números!");
                System.out.println(">>> O que é permitido: Apenas letras e espaços.");
                System.out.println(">>> Exemplo correto: MARIO SILVA. Tente novamente. <<<\n");
            } else {
                break;
            }
        }
        return nome.toUpperCase();
    }

    public static String lerCpfValido(Scanner scanner) {
        String cpf = "";
        while (true) {
            System.out.print("CPF (DIGITE APENAS OS 11 NÚMEROS) [Exemplo correto: 11144477735]: ");
            cpf = scanner.nextLine().trim().replaceAll("[^0-9]", "");

            if (cpf.length() != 11 || cpf.matches("^(.)\\1{10}$")) {
                System.out.println(">>> ERRO: CPF INVÁLIDO! O CPF deve conter 11 dígitos reais e não pode ser uma sequência repetida.");
                System.out.println(">>> O que é permitido: Apenas números válidos conforme receita federal.");
                System.out.println(">>> Exemplo correto: 11144477735. Tente novamente. <<<\n");
                continue;
            }

            try {
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
                    System.out.println(">>> ERRO: Os dígitos verificadores (DV) do CPF não conferem matematicamente!");
                    System.out.println(">>> Exemplo correto: Um CPF real e válido (Ex: 11144477735). Tente novamente. <<<\n");
                }
            } catch (Exception e) {
                System.out.println(">>> ERRO na validação do CPF. Tente novamente. <<<\n");
            }
        }
        return cpf;
    }

    public static String lerCepValido(Scanner scanner) {
        String cep = "";
        while (true) {
            System.out.print("CEP [Exemplo correto: 04753060]: ");
            cep = scanner.nextLine().trim().replaceAll("[^0-9]", "");

            if (cep.length() != 8) {
                System.out.println(">>> ERRO: CEP INVÁLIDO! O CEP deve conter exatamente 8 números.");
                System.out.println(">>> O que é permitido: Sequência exata de 8 dígitos numéricos.");
                System.out.println(">>> Exemplo correto: 04753060. Tente novamente. <<<\n");
            } else {
                break;
            }
        }
        return cep;
    }

    /* =========================================================================
       [MÓDULO 4] REGRA DE NEGÓCIO E GESTÃO SALARIAL
       -------------------------------------------------------------------------
       FONTE DAS INFORMAÇÕES: 
       - Padrões de mercado corporativo e tabelas salariais de referência para os cargos.
       ========================================================================= */
    public static double obterSalarioPadraoCargo(String cargo) {
        if (cargo.equalsIgnoreCase("GERENTE")) return 5500.00;
        if (cargo.equalsIgnoreCase("FINANCEIRO")) return 4800.00;
        return 2200.00; // OPERADOR
    }

    public static String verificarAlertaMediaSalarial(String cargo, double salario) {
        double min = 0.0, max = 0.0;
        if (cargo.equalsIgnoreCase("GERENTE")) {
            min = 4500.00; max = 7000.00;
        } else if (cargo.equalsIgnoreCase("FINANCEIRO")) {
            min = 3800.00; max = 5800.00;
        } else { // OPERADOR
            min = 1800.00; max = 3000.00;
        }

        if (salario < min) {
            return " [⚠ ALERTA: ABAIXO DA MÉDIA]";
        } else if (salario > max) {
            return " [⚠ ALERTA: ACIMA DA MÉDIA]";
        }
        return " [DENTRO DA MÉDIA]";
    }

    public static void exibirCabecalho() {
        System.out.println("==================================================");
        System.out.println("     SISTEMA DE GESTÃO TRIBUTÁVEL E ESTOQUE       ");
        System.out.println("==================================================");
    }

    /* =========================================================================
       MÉTODO PRINCIPAL (CONTROLE DE FLUXO E MENOS)
       ========================================================================= */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

          /* ===============================================================================================================
             TABELA DE AUDITORIA COMPLETA: ERROS ENCONTRADOS, CAUSAS E CORREÇÕES IMPLEMENTADAS NO SISTEMA
             ===============================================================================================================
             +-------------------------------+----------------------------------+------------------------------------------+
             | Erro / Problema Identificado  | Causa Raiz                       | Correção Implementada                    |
             +-------------------------------+----------------------------------+------------------------------------------+
             | Identificação Abrupta no      | O programa exigia o Nome e o CPF | Adiantamento condicional: o cliente      |
             | Painel do Cliente             | imediatamente ao entrar na opção | navega pelo catálogo e só é solicitado   |
             |                               | 1, antes de qualquer navegação.  | ao interagir com carrinho/compra.        |
             +-------------------------------+----------------------------------+------------------------------------------+
             | Falhas na Folha de Pagamento  | Ausência de controle individual  | Inclusão do vetor booleano `funcPago[]`  |
             |                               | de quitação salarial (risco de   | vinculado a cada funcionário para        |
             |                               | pagamentos em dobro).            | auditar pagamentos individuais.          |
             +-------------------------------+----------------------------------+------------------------------------------+
             | Falta de Exemplos nas         | Mensagens de erro puramente      | Padronização de todas as mensagens com   |
             | Entradas de Dados             | genéricas sem orientar formato.  | exemplos textuais claros (`[Exemplo...]`)|
             +-------------------------------+----------------------------------+------------------------------------------+
             | Carrinho Limitado e Dupla     | Ausência de rotinas de remoção e | Implementação de menu de revisão com     |
             | Baixa de Estoque              | ajuste de qtde; abatimento duplo.| remoção, alteração e baixa única no final.|
             +-------------------------------+----------------------------------+------------------------------------------+
             | Validação Frágil de CPF       | O sistema conferia apenas se a   | Implementação do algoritmo da Receita    |
             |                               | string tinha 11 caracteres.      | Federal (Módulo 11) com cálculo de DVs.  |
             +-------------------------------+----------------------------------+------------------------------------------+
             | Perda de Dados no Cadastro    | O cadastro no checkout era       | Inclusão de rotina de persistência para  |
             | Tardio                        | descartado após a compra.        | salvar permanentemente no array.         |
             +-------------------------------+----------------------------------+------------------------------------------+
             | Estouro de Limite de Vetores  | Inserção de dados além da        | Implementação de barreiras de proteção   |
             | (*ArrayIndexOutOfBounds*)     | capacidade máxima dos arrays.    | (*Bounds Checking*) em todos os cadastros|
             +-------------------------------+----------------------------------+------------------------------------------+ */

        // Arrays de Funcionários (Capacidade máxima: 10)
        String[] funcId = new String[10];
        String[] funcNome = new String[10];
        String[] funcCpf = new String[10];
        String[] funcCargo = new String[10]; 
        String[] funcSenha = new String[10];
        double[] funcSalario = new double[10];
        boolean[] funcPago = new boolean[10]; 
        int totalFuncionarios = 0;

        funcId[0] = "F01"; funcNome[0] = "CARLOS GERENTE"; funcCpf[0] = "35111584834"; funcCargo[0] = "GERENTE"; funcSenha[0] = "admin123"; funcSalario[0] = 5500.00; funcPago[0] = false;
        funcId[1] = "F02"; funcNome[1] = "ANA OPERADORA"; funcCpf[1] = "71493628030"; funcCargo[1] = "OPERADOR"; funcSenha[1] = "op123"; funcSalario[1] = 2200.00; funcPago[1] = false;
        funcId[2] = "F03"; funcNome[2] = "MARINA FINANCEIRO"; funcCpf[2] = "52998224725"; funcCargo[2] = "FINANCEIRO"; funcSenha[2] = "fin123"; funcSalario[2] = 4800.00; funcPago[2] = false;
        totalFuncionarios = 3;

        // Clientes (Capacidade máxima: 10)
        String[] cliNomeArr = new String[10];
        String[] cliCpfArr = new String[10];
        String[] cliSenhaArr = new String[10];
        String[] cliSexoArr = new String[10];
        String[] cliDataNascArr = new String[10];
        String[] cliUfArr = new String[10];
        String[] cliCepArr = new String[10];
        int totalClientes = 0;

        cliNomeArr[0] = "MARIO SILVA";
        cliCpfArr[0] = "11144477735";
        cliSenhaArr[0] = "cli123";
        cliSexoArr[0] = "M";
        cliDataNascArr[0] = "10/05/1995";
        cliUfArr[0] = "SP";
        cliCepArr[0] = "04753060";
        totalClientes = 1;

        // Produtos (Capacidade máxima: 20)
        String[] prodNome = new String[20];
        String[] prodNcm = new String[20];
        String[] prodCest = new String[20];
        String[] prodCfop = new String[20];
        String[] prodCst = new String[20];
        double[] prodPrecos = new double[20];
        double[] prodPrecoOriginal = new double[20];
        int[] prodEstoque = new int[20];
        int[] prodCategoria = new int[20];
        int totalProdutos = 0;

        prodNome[0] = "WHEY PROTEIN ISOLADO"; prodCategoria[0] = 1; prodPrecos[0] = 120.00; prodPrecoOriginal[0] = 120.00; prodEstoque[0] = 50; prodNcm[0] = "2106.90.30"; prodCest[0] = "NULO"; prodCfop[0] = "5102"; prodCst[0] = "00";
        prodNome[1] = "BEBIDA ISOTÔNICA 500ML"; prodCategoria[1] = 2; prodPrecos[1] = 8.50; prodPrecoOriginal[1] = 10.00; prodEstoque[1] = 100; prodNcm[1] = "2202.99.00"; prodCest[1] = "NULO"; prodCfop[1] = "5102"; prodCst[1] = "60";
        prodNome[2] = "CAMISA ESPORTIVA DRY-FIT"; prodCategoria[2] = 3; prodPrecos[2] = 59.90; prodPrecoOriginal[2] = 59.90; prodEstoque[2] = 30; prodNcm[2] = "6109.90.00"; prodCest[2] = "28.045.00"; prodCfop[2] = "5102"; prodCst[2] = "00";
        prodNome[3] = "TÊNIS DE CORRIDA PERFORMANCE"; prodCategoria[3] = 4; prodPrecos[3] = 249.90; prodPrecoOriginal[3] = 299.90; prodEstoque[3] = 15; prodNcm[3] = "6404.11.00"; prodCest[3] = "28.058.00"; prodCfop[3] = "5102"; prodCst[3] = "00";
        prodNome[4] = "CREATINA MONOHIDRATADA 250G"; prodCategoria[4] = 1; prodPrecos[4] = 89.90; prodPrecoOriginal[4] = 89.90; prodEstoque[4] = 40; prodNcm[4] = "2106.90.90"; prodCest[4] = "NULO"; prodCfop[4] = "5102"; prodCst[4] = "00";
        prodNome[5] = "BARRA DE PROTEÍNA CHOCOLATE"; prodCategoria[5] = 1; prodPrecos[5] = 9.00; prodPrecoOriginal[5] = 10.00; prodEstoque[5] = 120; prodNcm[5] = "2106.90.30"; prodCest[5] = "NULO"; prodCfop[5] = "5102"; prodCst[5] = "00";
        prodNome[6] = "ENERGÉTICO LATA 473ML"; prodCategoria[6] = 2; prodPrecos[6] = 11.00; prodPrecoOriginal[6] = 11.00; prodEstoque[6] = 80; prodNcm[6] = "2202.99.00"; prodCest[6] = "NULO"; prodCfop[6] = "5102"; prodCst[6] = "60";
        prodNome[7] = "ÁGUA MINERAL 500ML SEM GÁS"; prodCategoria[7] = 2; prodPrecos[7] = 3.50; prodPrecoOriginal[7] = 3.50; prodEstoque[7] = 200; prodNcm[7] = "2201.10.00"; prodCest[7] = "NULO"; prodCfop[7] = "5102"; prodCst[7] = "00";
        prodNome[8] = "BERMUDA TÉRMICA COMPRESSÃO"; prodCategoria[8] = 3; prodPrecos[8] = 79.90; prodPrecoOriginal[8] = 89.90; prodEstoque[8] = 25; prodNcm[8] = "6112.19.00"; prodCest[8] = "28.045.00"; prodCfop[8] = "5102"; prodCst[8] = "00";
        prodNome[9] = "MEIAS ESPORTIVAS CANO ALTO (3UN)"; prodCategoria[9] = 3; prodPrecos[9] = 34.90; prodPrecoOriginal[9] = 34.90; prodEstoque[9] = 50; prodNcm[9] = "6115.96.00"; prodCest[9] = "28.047.00"; prodCfop[9] = "5102"; prodCst[9] = "00";
        prodNome[10] = "CHINELO SLIDE ANATÔMICO"; prodCategoria[10] = 4; prodPrecos[10] = 69.90; prodPrecoOriginal[10] = 69.90; prodEstoque[10] = 30; prodNcm[10] = "6402.20.00"; prodCest[10] = "28.058.00"; prodCfop[10] = "5102"; prodCst[10] = "00";
        prodNome[11] = "MOCHILA ESPORTIVA IMPERMEÁVEL"; prodCategoria[11] = 5; prodPrecos[11] = 159.90; prodPrecoOriginal[11] = 179.90; prodEstoque[11] = 20; prodNcm[11] = "4202.92.00"; prodCest[11] = "12.001.00"; prodCfop[11] = "5102"; prodCst[11] = "00";
        prodNome[12] = "GARRAFA SQUEEZE 1L INOX"; prodCategoria[12] = 5; prodPrecos[12] = 49.90; prodPrecoOriginal[12] = 49.90; prodEstoque[12] = 45; prodNcm[12] = "7323.93.00"; prodCest[12] = "22.001.00"; prodCfop[12] = "5102"; prodCst[12] = "00";
        prodNome[13] = "LUVA DE TREINO MUSCULAÇÃO"; prodCategoria[13] = 5; prodPrecos[13] = 45.00; prodPrecoOriginal[13] = 50.00; prodEstoque[13] = 35; prodNcm[13] = "4203.29.00"; prodCest[13] = "28.060.00"; prodCfop[13] = "5102"; prodCst[13] = "00";
        totalProdutos = 14;

        // Fornecedores (Capacidade máxima: 5)
        String[] fornNome = new String[5]; 
        String[] fornCnpj = new String[5]; 
        String[] fornUf = new String[5]; 
        String[] fornProdutoFornecido = new String[5];
        double[] fornPrecoPago = new double[5];
        double[] fornLucroPretendidoPct = new double[5]; 
        int totalFornecedores = 0;

        fornNome[0] = "SUPLEMAX DISTRIBUIDORA LTDA"; fornCnpj[0] = "12345678000190"; fornUf[0] = "SP"; 
        fornProdutoFornecido[0] = "WHEY PROTEIN ISOLADO"; fornPrecoPago[0] = 80.00; fornLucroPretendidoPct[0] = 50.0;
        totalFornecedores = 1;

        double saldoCaixaLoja = 100000.00;   
        double totalGastosEstoque = 15000.00; 
        double totalReceitaVendas = 0.0;     
        double totalSalariosPagos = 0.0;     

        String ufOrigem = "SP";
        int anoExercicioPadrao = 2026;

        boolean sistemaAtivo = true;
        while (sistemaAtivo) {
            exibirCabecalho();
            System.out.println("IDENTIFIQUE SEU PERFIL DE ACESSO:");
            System.out.println("1 - CLIENTE (Realizar Compras / Carrinho)");
            System.out.println("2 - FUNCIONÁRIO (Painel Administrativo / Login)");
            System.out.println("3 - SAIR COMPLETAMENTE DO SISTEMA");
            System.out.print("ESCOLHA UMA OPÇÃO [Exemplo correto: 1]: ");

            int perfilAcesso = 0;
            try {
                perfilAcesso = scanner.nextInt();
                scanner.nextLine();
            } catch (InputMismatchException e) {
                System.out.println(">>> ERRO: OPÇÃO INVÁLIDA! Digite apenas números inteiros.");
                System.out.println(">>> O que é permitido: Números de 1 a 3.");
                System.out.println(">>> Exemplo correto: 1. Tente novamente. <<<\n");
                scanner.nextLine();
                continue;
            }

            if (perfilAcesso == 3) {
                sistemaAtivo = false;
                break;
            } else if (perfilAcesso < 1 || perfilAcesso > 2) {
                System.out.println(">>> ERRO: Opção inexistente!");
                System.out.println(">>> O que é permitido: Escolher entre 1, 2 ou 3.");
                System.out.println(">>> Exemplo correto: 1 ou 2. Tente novamente. <<<\n");
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

                int[] carrinhoProdIndex = new int[10];
                int[] carrinhoQtde = new int[10];
                int totalItensCarrinho = 0;

                int opcaoCliente = 0;
                while (opcaoCliente != 5) {
                    System.out.println("\n=================== PAINEL DO CLIENTE ===================");
                    System.out.println("1 - VISUALIZAR CATÁLOGO DE PRODUTOS");
                    System.out.println("2 - ADICIONAR PRODUTO AO CARRINHO DE COMPRAS");
                    System.out.println("3 - VISUALIZAR CARRINHO / REVISAR / FINALIZAR COMPRA");
                    System.out.println("4 - VOLTAR À ABA ANTERIOR (TROCAR DE PERFIL)");
                    System.out.println("5 - SAIR DO SISTEMA");
                    System.out.print("ESCOLHA UMA OPÇÃO [Exemplo correto: 1]: ");

                    try {
                        opcaoCliente = scanner.nextInt();
                        scanner.nextLine();
                    } catch (InputMismatchException e) {
                        System.out.println(">>> ERRO: Digite apenas números inteiros!");
                        System.out.println(">>> O que é permitido: Números de 1 a 5.");
                        System.out.println(">>> Exemplo correto: 1. Tente novamente. <<<\n");
                        scanner.nextLine();
                        continue;
                    }

                    if ((opcaoCliente == 2 || opcaoCliente == 3) && !clienteIdentificado) {
                        System.out.println("\n--- IDENTIFICAÇÃO DO CLIENTE ---");
                        cliNome = lerNomeValido(scanner, "NOME COMPLETO [Exemplo correto: MARIA SOUZA]: ");
                        cliCpf = lerCpfValido(scanner);

                        int clienteIdx = -1;
                        for (int c = 0; c < totalClientes; c++) {
                            if (cliCpfArr[c].equals(cliCpf)) {
                                clienteIdx = c;
                                break;
                            }
                        }

                        if (clienteIdx != -1) {
                            System.out.println(">>> CPF JÁ CADASTRADO NO SISTEMA (" + cliNomeArr[clienteIdx] + ")! <<<");
                            System.out.print("DIGITE SUA SENHA DE ACESSO [Exemplo correto: cli123]: ");
                            scanner.nextLine(); 
                            cliNome = cliNomeArr[clienteIdx];
                            cliUf = cliUfArr[clienteIdx];
                            cliCep = cliCepArr[clienteIdx];
                            clienteCadastrado = true;
                        } else {
                            System.out.println(">>> CPF não encontrado no cadastro. O que deseja fazer? <<<");
                            System.out.println("1 - Realizar cadastro completo agora");
                            System.out.println("2 - Ir direto para a compra (Cadastrar apenas na finalização)");
                            System.out.print("ESCOLHA [Exemplo correto: 1]: ");
                            int optCliNovo = 0;
                            try {
                                optCliNovo = scanner.nextInt();
                                scanner.nextLine();
                            } catch (InputMismatchException e) {
                                scanner.nextLine();
                                optCliNovo = 2;
                            }

                            if (optCliNovo == 1) {
                                System.out.print("SEXO [Exemplo correto: M]: "); scanner.nextLine();
                                System.out.print("DATA DE NASCIMENTO [Exemplo correto: 10/05/1995]: "); scanner.nextLine();
                                System.out.print("UF DE RESIDÊNCIA [Exemplo correto: SP]: "); cliUf = scanner.nextLine().toUpperCase();
                                cliCep = lerCepValido(scanner);
                                String cliSenha = geradorSenhaAleatoriaSistema();
                                System.out.println(">>> SENHA GERADA PARA PRÓXIMOS ACESSOS: " + cliSenha + " <<<");

                                if (totalClientes < cliNomeArr.length) {
                                    cliNomeArr[totalClientes] = cliNome;
                                    cliCpfArr[totalClientes] = cliCpf;
                                    cliSenhaArr[totalClientes] = cliSenha;
                                    cliSexoArr[totalClientes] = "M";
                                    cliDataNascArr[totalClientes] = "01/01/2000";
                                    cliUfArr[totalClientes] = cliUf;
                                    cliCepArr[totalClientes] = cliCep;
                                    totalClientes++;
                                } else {
                                    System.out.println(">>> AVISO: Limite máximo de clientes atingido. Cadastro não persistido na base. <<<");
                                }
                                clienteCadastrado = true;
                            } else {
                                System.out.println(">>> Suas informações cadastrais completas serão solicitadas ao concluir a compra. <<<");
                                clienteCadastrado = false;
                            }
                        }
                        clienteIdentificado = true;
                        System.out.println("\n>>> BEM-VINDO(A), " + cliNome.toUpperCase() + "! <<<");
                    }

                    if (opcaoCliente == 1) {
                        System.out.println("\n--- CATÁLOGO DE PRODUTOS DISPONÍVEIS ---");
                        for (int i = 0; i < totalProdutos; i++) {
                            if (prodNome[i] != null) {
                                String statusEstoque = (prodEstoque[i] > 0) ? " | Estoque: " + prodEstoque[i] : " | [ESGOTADO]";
                                String tagOferta = (prodPrecos[i] < prodPrecoOriginal[i]) ? " ** [EM OFERTA!] **" : "";
                                System.out.println("ID: " + (i + 1) + " | PRODUTO: " + prodNome[i] + 
                                        " | PREÇO: " + formatarMoeda(prodPrecos[i]) + tagOferta + statusEstoque);
                            }
                        }
                    } else if (opcaoCliente == 2) {
                        System.out.println("\n--- ADICIONAR PRODUTOS AO CARRINHO ---");
                        boolean temProdutosDisponiveis = false;
                        for (int i = 0; i < totalProdutos; i++) {
                            if (prodNome[i] != null && prodEstoque[i] > 0) {
                                String tagOferta = (prodPrecos[i] < prodPrecoOriginal[i]) ? " ** [EM OFERTA!] **" : "";
                                System.out.println("ID: " + (i + 1) + " | " + prodNome[i] + " - " + formatarMoeda(prodPrecos[i]) + tagOferta + " | Disponível: " + prodEstoque[i]);
                                temProdutosDisponiveis = true;
                            }
                        }

                        if (!temProdutosDisponiveis) {
                            System.out.println(">>> ATENÇÃO: Não há produtos disponíveis em estoque no momento! <<<");
                        } else {
                            System.out.print("DIGITE O ID DO PRODUTO [Exemplo correto: 1]: ");
                            int posItem = -1;
                            try {
                                posItem = scanner.nextInt() - 1;
                                scanner.nextLine();
                            } catch (InputMismatchException e) {
                                scanner.nextLine();
                                System.out.println(">>> ERRO: Digite um número de ID válido! <<<\n");
                                continue;
                            }

                            if (posItem >= 0 && posItem < totalProdutos && prodNome[posItem] != null && prodEstoque[posItem] > 0) {
                                System.out.print("DIGITE A QUANTIDADE DESEJADA [Exemplo correto: 2]: ");
                                int qtdeComprar = 0;
                                try {
                                    qtdeComprar = scanner.nextInt();
                                    scanner.nextLine();
                                } catch (InputMismatchException e) {
                                    scanner.nextLine();
                                    System.out.println(">>> ERRO: Quantidade inválida! <<<\n");
                                    continue;
                                }

                                if (qtdeComprar > 0 && qtdeComprar <= prodEstoque[posItem]) {
                                    if (totalItensCarrinho < carrinhoProdIndex.length) {
                                        carrinhoProdIndex[totalItensCarrinho] = posItem;
                                        carrinhoQtde[totalItensCarrinho] = qtdeComprar;
                                        totalItensCarrinho++;
                                        System.out.println(">>> PRODUTO ADICIONADO AO CARRINHO COM SUCESSO! <<<");
                                    } else {
                                        System.out.println(">>> CARRINHO CHEIO! <<<");
                                    }
                                } else if (qtdeComprar > prodEstoque[posItem]) {
                                    System.out.println(">>> ESTOQUE INSUFICIENTE! Quantidade excede o estoque disponível (" + prodEstoque[posItem] + "). <<<");
                                } else {
                                    System.out.println(">>> QUANTIDADE INVÁLIDA! O valor deve ser maior que zero. <<<");
                                }
                            } else {
                                System.out.println(">>> ERRO: ID de produto inválido ou inexistente! Escolha um ID listado no catálogo. <<<");
                            }
                        }
                    } else if (opcaoCliente == 3) {
                        if (totalItensCarrinho == 0) {
                            System.out.println("\nSEU CARRINHO ESTÁ VAZIO.");
                        } else {
                            boolean revisandoCarrinho = true;
                            while (revisandoCarrinho) {
                                double valorTotalCarrinho = 0.0;
                                System.out.println("\n==================================================");
                                System.out.println("         REVISÃO DO CARRINHO DE COMPRAS           ");
                                System.out.println("==================================================");
                                for (int i = 0; i < totalItensCarrinho; i++) {
                                    int idxP = carrinhoProdIndex[i];
                                    double valItem = prodPrecos[idxP] * carrinhoQtde[i];
                                    valorTotalCarrinho += valItem;
                                    System.out.printf("[%d] %s | Qtde: %d | Unit: %s | Total: %s\n", 
                                            (i + 1), prodNome[idxP], carrinhoQtde[i], formatarMoeda(prodPrecos[idxP]), formatarMoeda(valItem));
                                }
                                System.out.println("--------------------------------------------------");
                                System.out.println("VALOR TOTAL DO CARRINHO: " + formatarMoeda(valorTotalCarrinho));

                                System.out.println("\n1 - Concluir compra e prosseguir para pagamento");
                                System.out.println("2 - Alterar quantidade de um item");
                                System.out.println("3 - Remover um item do carrinho");
                                System.out.println("4 - Voltar ao menu do cliente");
                                System.out.print("ESCOLHA [Exemplo correto: 1]: ");
                                int acaoCarrinho = 0;
                                try {
                                    acaoCarrinho = scanner.nextInt();
                                    scanner.nextLine();
                                } catch (InputMismatchException e) {
                                    scanner.nextLine();
                                    acaoCarrinho = 4;
                                }

                                if (acaoCarrinho == 1) {
                                    if (!clienteCadastrado) {
                                        System.out.println("\n==================================================");
                                        System.out.println(" >>> AVISO OBRIGATÓRIO DE CADASTRO PARA FATURAMENTO <<<");
                                        System.out.println("==================================================");
                                        System.out.println(" Para concluir a compra e emitir a Nota Fiscal exigida pela ");
                                        System.out.println(" Receita Federal, precisamos requisitar seu endereço e dados.");
                                        System.out.println("--------------------------------------------------");
                                        
                                        System.out.print("INFORME SEU SEXO [Exemplo correto: M]: "); String sexoCli = scanner.nextLine();
                                        System.out.print("DATA DE NASCIMENTO [Exemplo correto: 10/05/1995]: "); String nascCli = scanner.nextLine();
                                        System.out.print("UF DE RESIDÊNCIA [Exemplo correto: SP]: "); cliUf = scanner.nextLine().toUpperCase();
                                        cliCep = lerCepValido(scanner);
                                        String cliSenhaGerada = geradorSenhaAleatoriaSistema();

                                        if (totalClientes < cliNomeArr.length) {
                                            cliNomeArr[totalClientes] = cliNome;
                                            cliCpfArr[totalClientes] = cliCpf;
                                            cliSenhaArr[totalClientes] = cliSenhaGerada;
                                            cliSexoArr[totalClientes] = sexoCli;
                                            cliDataNascArr[totalClientes] = nascCli;
                                            cliUfArr[totalClientes] = cliUf;
                                            cliCepArr[totalClientes] = cliCep;
                                            totalClientes++;
                                        }
                                        clienteCadastrado = true;
                                        System.out.println(">>> CADASTRO EFETIVADO COM SUCESSO! SENHA DE ACESSO: " + cliSenhaGerada + " <<<");
                                    }

                                    System.out.println("\n--- SELECIONE A FORMA DE PAGAMENTO ---");
                                    System.out.println("1 - PIX (10% de Desconto)");
                                    System.out.println("2 - Cartão de Crédito à Vista");
                                    System.out.println("3 - Cartão Parcelado em 3x (5% de acréscimo)");
                                    System.out.print("ESCOLHA [Exemplo correto: 1]: ");
                                    int pagto = 1;
                                    try {
                                        pagto = scanner.nextInt();
                                        scanner.nextLine();
                                    } catch (InputMismatchException e) {
                                        scanner.nextLine();
                                        pagto = 1;
                                    }

                                    double valFinal = valorTotalCarrinho;
                                    if (pagto == 1) valFinal *= 0.90;
                                    else if (pagto == 3) valFinal *= 1.05;

                                    revisandoCarrinho = false;
                                    totalReceitaVendas += valFinal; 
                                    saldoCaixaLoja += valFinal; 

                                    // Baixa única de estoque efetuada no checkout
                                    for (int i = 0; i < totalItensCarrinho; i++) {
                                        prodEstoque[carrinhoProdIndex[i]] -= carrinhoQtde[i];
                                    }

                                    System.out.println("\n>>> NOTA FISCAL EMITIDA COM SUCESSO PARA " + cliNome.toUpperCase() + "! <<<");
                                    System.out.println("VALOR FINAL PAGO: " + formatarMoeda(valFinal));
                                    totalItensCarrinho = 0;
                                } else if (acaoCarrinho == 2) {
                                    System.out.print("DIGITE O NÚMERO DO ITEM NO CARRINHO [Exemplo correto: 1]: ");
                                    int idxAlt = scanner.nextInt() - 1;
                                    scanner.nextLine();
                                    if (idxAlt >= 0 && idxAlt < totalItensCarrinho) {
                                        System.out.print("NOVA QUANTIDADE [Exemplo correto: 3]: ");
                                        int novaQtde = scanner.nextInt();
                                        scanner.nextLine();
                                        int idxP = carrinhoProdIndex[idxAlt];
                                        if (novaQtde > 0 && novaQtde <= prodEstoque[idxP]) {
                                            carrinhoQtde[idxAlt] = novaQtde;
                                            System.out.println(">>> QUANTIDADE ATUALIZADA NO CARRINHO! <<<");
                                        } else {
                                            System.out.println(">>> ERRO: Quantidade excede o estoque disponível! <<<");
                                        }
                                    } else {
                                        System.out.println(">>> ERRO: Item inválido no carrinho! <<<");
                                    }
                                } else if (acaoCarrinho == 3) {
                                    System.out.print("DIGITE O NÚMERO DO ITEM QUE DESEJA REMOVER [Exemplo correto: 1]: ");
                                    int idxRem = scanner.nextInt() - 1;
                                    scanner.nextLine();
                                    if (idxRem >= 0 && idxRem < totalItensCarrinho) {
                                        for (int i = idxRem; i < totalItensCarrinho - 1; i++) {
                                            carrinhoProdIndex[i] = carrinhoProdIndex[i + 1];
                                            carrinhoQtde[i] = carrinhoQtde[i + 1];
                                        }
                                        totalItensCarrinho--;
                                        System.out.println(">>> ITEM REMOVIDO DO CARRINHO COM SUCESSO! <<<");
                                    } else {
                                        System.out.println(">>> ERRO: Item inválido no carrinho! <<<");
                                    }
                                } else {
                                    revisandoCarrinho = false;
                                }
                            }
                        }
                    } else if (opcaoCliente == 4) {
                        break;
                    } else if (opcaoCliente == 5) {
                        sistemaAtivo = false;
                        break;
                    } else {
                        System.out.println(">>> ERRO: Opção inexistente no painel do cliente!");
                        System.out.println(">>> O que é permitido: Escolher entre 1 e 5.");
                        System.out.println(">>> Exemplo correto: 1. Tente novamente. <<<\n");
                    }
                }

            /* =========================================================================
               FLUXO DO FUNCIONÁRIO
               ========================================================================= */
            } else if (perfilAcesso == 2) {
                System.out.println("\n--- LOGIN DO FUNCIONÁRIO ---");
                String cpfLogin = lerCpfValido(scanner);

                int funcLogadoIndex = -1;
                for (int i = 0; i < totalFuncionarios; i++) {
                    if (funcCpf[i].equals(cpfLogin)) {
                        funcLogadoIndex = i;
                        break;
                    }
                }

                if (funcLogadoIndex == -1) {
                    System.out.println(">>> ERRO: CPF não cadastrado no sistema!");
                    System.out.println(">>> O que é permitido: Inserir um CPF válido e cadastrado.");
                    System.out.println(">>> Exemplo correto: 35111584834. Retornando ao menu principal. <<<\n");
                    continue;
                }

                System.out.println(">>> Usuário reconhecido: ID [" + funcId[funcLogadoIndex] + "] " + funcNome[funcLogadoIndex] + " (" + funcCargo[funcLogadoIndex] + ") <<<");

                int tentativas = 3;
                boolean acessoLiberado = false;

                while (tentativas > 0) {
                    System.out.print("DIGITE SUA SENHA [Exemplo correto: admin123]: ");
                    String senhaLogin = scanner.nextLine();

                    if (funcSenha[funcLogadoIndex].equals(senhaLogin)) {
                        acessoLiberado = true;
                        break;
                    } else {
                        tentativas--;
                        if (tentativas > 0) {
                            System.out.println(">>> SENHA INCORRETA! Restam " + tentativas + " tentativa(s).");
                            System.out.println(">>> O que é permitido: Digitar a senha correta cadastrada.");
                            System.out.println(">>> Exemplo correto: admin123. Tente novamente. <<<\n");
                        } else {
                            System.out.println("\n==================================================");
                            System.out.println(" >>> BLOQUEIO DE SEGURANÇA: 3 TENTATIVAS FALHADAS! <<<");
                            System.out.println("==================================================");
                            System.out.println(" O limite máximo de tentativas de senha foi atingido. ");
                            System.out.println(" Para desbloquear o acesso, é necessária a intervenção ");
                            System.out.println(" de uma autoridade superior (Gerente).");
                            System.out.println("--------------------------------------------------");
                            
                            System.out.print("DIGITE O ID DO GERENTE [Exemplo correto: F01]: ");
                            String idGerenteSup = scanner.nextLine().trim().toUpperCase();
                            System.out.print("DIGITE A SENHA DO GERENTE [Exemplo correto: admin123]: ");
                            String senhaGerenteSup = scanner.nextLine();

                            int gerenciaIndex = -1;
                            for (int g = 0; g < totalFuncionarios; g++) {
                                if (funcId[g].equals(idGerenteSup) && funcCargo[g].equals("GERENTE")) {
                                    gerenciaIndex = g;
                                    break;
                                }
                            }

                            if (gerenciaIndex != -1 && funcSenha[gerenciaIndex].equals(senhaGerenteSup)) {
                                System.out.println(">>> ACESSO LIBERADO COM SUCESSO PELA AUTORIDADE SUPERIOR (" + funcNome[gerenciaIndex] + ")! <<<");
                                acessoLiberado = true;
                                break;
                            } else {
                                System.out.println(">>> ERRO: Credenciais de autoridade inválidas ou cargo não autorizado!");
                                System.out.println(">>> O que é permitido: Fornecer ID de um Gerente ativo e senha correta.");
                                System.out.println(">>> Exemplo correto: ID: F01 e Senha: admin123. Retornando ao menu inicial. <<<\n");
                                break;
                            }
                        }
                    }
                }

                if (!acessoLiberado) {
                    continue;
                }

                String cargoAtual = funcCargo[funcLogadoIndex];
                System.out.println("\n>>> PAINEL OPERACIONAL: ID [" + funcId[funcLogadoIndex] + "] " + funcNome[funcLogadoIndex].toUpperCase() 
                        + " [" + cargoAtual + "] <<<");

                int opcaoMenu = 0;
                while (opcaoMenu != 13) {
                    System.out.println("\n=================== MENU ADMINISTRATIVO ===================");

                    if (cargoAtual.equals("GERENTE")) {
                        System.out.println("1 - Cadastrar Novo Funcionário");
                        System.out.println("2 - Cadastrar / Gerenciar Fornecedores");
                        System.out.println("3 - Cadastrar Produto e Ficha Fiscal");
                        System.out.println("4 - Ajustar Preços e Aplicar Descontos");
                        System.out.println("5 - Movimentação de Estoque / Custos de Abastecimento");
                        System.out.println("6 - Pagar Folha de Salários");
                        System.out.println("7 - Configurar Impostos (Ano Atual / Próximo)");
                        System.out.println("8 - Emitir Nota Fiscal de Saída");
                        System.out.println("9 - Painel Financeiro, Lucro e Balanço de Caixa");
                        System.out.println("10 - Listar Funcionários Cadastrados");
                        System.out.println("11 - Alterar Minha Senha");
                        System.out.println("12 - Voltar à Aba Anterior (Trocar Perfil)");
                        System.out.println("13 - Sair do Sistema");
                    } else if (cargoAtual.equals("FINANCEIRO")) {
                        System.out.println("2 - Cadastrar / Gerenciar Fornecedores");
                        System.out.println("5 - Movimentação de Estoque / Custos de Abastecimento");
                        System.out.println("6 - Pagar Folha de Salários");
                        System.out.println("7 - Configurar Impostos (Ano Atual / Próximo)");
                        System.out.println("8 - Emitir Nota Fiscal de Saída");
                        System.out.println("9 - Painel Financeiro, Lucro e Balanço de Caixa");
                        System.out.println("10 - Listar Funcionários Cadastrados");
                        System.out.println("11 - Alterar Minha Senha");
                        System.out.println("12 - Voltar à Aba Anterior (Trocar Perfil)");
                        System.out.println("13 - Sair do Sistema");
                    } else if (cargoAtual.equals("OPERADOR")) {
                        System.out.println("3 - Cadastrar Produto e Ficha Fiscal");
                        System.out.println("4 - Ajustar Preços e Aplicar Descontos");
                        System.out.println("5 - Movimentação de Estoque / Custos de Abastecimento");
                        System.out.println("8 - Emitir Nota Fiscal de Saída");
                        System.out.println("10 - Listar Funcionários Cadastrados");
                        System.out.println("11 - Alterar Minha Senha");
                        System.out.println("12 - Voltar à Aba Anterior (Trocar Perfil)");
                        System.out.println("13 - Sair do Sistema");
                    }

                    System.out.print("ESCOLHA UMA OPÇÃO PERMITIDA [Exemplo correto: 1]: ");

                    try {
                        opcaoMenu = scanner.nextInt();
                        scanner.nextLine();
                    } catch (InputMismatchException e) {
                        System.out.println(">>> ERRO: Digite apenas números inteiros válidos!");
                        System.out.println(">>> O que é permitido: Números de 1 a 13.");
                        System.out.println(">>> Exemplo correto: 1. Tente novamente. <<<\n");
                        scanner.nextLine();
                        continue;
                    }

                    // VALIDAÇÕES DE ACESSO POR CARGO
                    if (opcaoMenu == 1 && !cargoAtual.equals("GERENTE")) {
                        System.out.println("\n>>> [ACESSO NEGADO]: Apenas GERENTES possuem autoridade para Cadastrar Funcionários! <<<");
                        continue;
                    }
                    if ((opcaoMenu == 2 || opcaoMenu == 6 || opcaoMenu == 7 || opcaoMenu == 9) && cargoAtual.equals("OPERADOR")) {
                        System.out.println("\n>>> [ACESSO NEGADO]: O cargo de OPERADOR não possui autoridade para acessar funções financeiras ou fiscais! <<<");
                        continue;
                    }
                    if ((opcaoMenu == 3 || opcaoMenu == 4) && cargoAtual.equals("FINANCEIRO")) {
                        System.out.println("\n>>> [ACESSO NEGADO]: O setor FINANCEIRO não gerencia catálogo e preços de produtos! <<<");
                        continue;
                    }

                    if (opcaoMenu == 1) {
                        if (totalFuncionarios >= funcId.length) {
                            System.out.println(">>> ERRO: Limite máximo de funcionários atingido no sistema (" + funcId.length + ")! <<<\n");
                            continue;
                        }

                        System.out.println("\n--- CADASTRO DE NOVO FUNCIONÁRIO ---");
                        System.out.print("INFORME O ID FUNCIONAL EXCLUSIVO [Exemplo correto: F04]: "); 
                        String novoId = scanner.nextLine().trim().toUpperCase();
                        String novoNome = lerNomeValido(scanner, "NOME [Exemplo correto: CARLOS SOUZA]: ");
                        String novoCpf = lerCpfValido(scanner);

                        funcId[totalFuncionarios] = novoId;
                        funcNome[totalFuncionarios] = novoNome;
                        funcCpf[totalFuncionarios] = novoCpf;
                        funcSenha[totalFuncionarios] = geradorSenhaAleatoriaSistema();
                        funcPago[totalFuncionarios] = false; 

                        System.out.print("CARGO [1 - GERENTE | 2 - OPERADOR | 3 - FINANCEIRO] [Exemplo correto: 2]: ");
                        int cOpt = 2;
                        try {
                            cOpt = scanner.nextInt();
                            scanner.nextLine();
                        } catch (InputMismatchException e) {
                            scanner.nextLine();
                            cOpt = 2;
                        }
                        
                        String cargoDefinido = "OPERADOR";
                        if (cOpt == 1) cargoDefinido = "GERENTE";
                        else if (cOpt == 3) cargoDefinido = "FINANCEIRO";
                        
                        funcCargo[totalFuncionarios] = cargoDefinido;

                        double salarioPadrao = obterSalarioPadraoCargo(cargoDefinido);
                        funcSalario[totalFuncionarios] = salarioPadrao;

                        System.out.println(">>> SENHA GERADA AUTOMATICAMENTE: " + funcSenha[totalFuncionarios] + " <<<");
                        System.out.println(">>> SALÁRIO PADRÃO DO CARGO ATRIBUÍDO: " + formatarMoeda(salarioPadrao) + " <<<");

                        totalFuncionarios++;
                        System.out.println(">>> FUNCIONÁRIO CADASTRADO COM SUCESSO SOB O ID [" + novoId + "]! <<<");

                    } else if (opcaoMenu == 2) {
                        if (totalFornecedores >= fornNome.length) {
                            System.out.println(">>> ERRO: Limite máximo de fornecedores atingido no sistema (" + fornNome.length + ")! <<<\n");
                            continue;
                        }

                        System.out.println("\n--- CADASTRO DE FORNECEDOR E POLÍTICA DE PREÇOS ---");
                        System.out.print("NOME / RAZÃO SOCIAL [Exemplo correto: SUPLEMAX LTDA]: "); fornNome[totalFornecedores] = scanner.nextLine();
                        System.out.print("CNPJ [Exemplo correto: 12345678000190]: "); fornCnpj[totalFornecedores] = scanner.nextLine();
                        System.out.print("UF [Exemplo correto: SP]: "); fornUf[totalFornecedores] = scanner.nextLine().toUpperCase();
                        System.out.print("PRODUTO QUE FORNECE [Exemplo correto: WHEY PROTEIN]: "); fornProdutoFornecido[totalFornecedores] = scanner.nextLine();
                        System.out.print("PREÇO PAGO AO FORNECEDOR (CUSTO R$) [Exemplo correto: 80.00]: "); fornPrecoPago[totalFornecedores] = scanner.nextDouble();
                        System.out.print("LUCRO PRETENDIDO PARA O MERCADO (% sobre o custo) [Exemplo correto: 50.0]: "); fornLucroPretendidoPct[totalFornecedores] = scanner.nextDouble();
                        scanner.nextLine();

                        totalFornecedores++;
                        System.out.println(">>> FORNECEDOR CADASTRADO COM SUCESSO! <<<");

                    } else if (opcaoMenu == 3) {
                        if (totalProdutos >= prodNome.length) {
                            System.out.println(">>> ERRO: Limite máximo de produtos atingido no sistema (" + prodNome.length + ")! <<<\n");
                            continue;
                        }

                        System.out.println("\n--- CADASTRO DO PRODUTO ---");
                        System.out.print("DIGITE O ID (POSIÇÃO DESEJADA DE 1 A 20) [Exemplo correto: 5]: ");
                        int idDesejado = -1;
                        try {
                            idDesejado = scanner.nextInt() - 1;
                        } catch (InputMismatchException e) {
                            scanner.nextLine();
                            System.out.println(">>> ERRO: ID inválido! <<<\n");
                            continue;
                        }

                        if (idDesejado >= 0 && idDesejado < prodNome.length) {
                            if (prodNome[idDesejado] != null) {
                                System.out.println(">>> AVISO: O ID " + (idDesejado + 1) + " já está sendo utilizado por '" + prodNome[idDesejado] + "'!");
                                System.out.println(">>> O que é permitido: Escolher uma posição vazia.");
                                System.out.println(">>> Exemplo correto: 5. Tente novamente. <<<\n");
                                scanner.nextLine();
                                continue;
                            }

                            scanner.nextLine(); 
                            System.out.print("NOME DO PRODUTO [Exemplo correto: VITAMINA C]: ");
                            String nomeNovoProd = scanner.nextLine().trim();

                            boolean nomeDuplicado = false;
                            for (int p = 0; p < totalProdutos; p++) {
                                if (prodNome[p] != null && prodNome[p].equalsIgnoreCase(nomeNovoProd)) {
                                    nomeDuplicado = true;
                                    break;
                                }
                            }

                            if (nomeDuplicado) {
                                System.out.println(">>> ERRO: O nome do produto '" + nomeNovoProd + "' já existe no sistema!");
                                System.out.println(">>> O que é permitido: Digitar um nome exclusivo.");
                                System.out.println(">>> Exemplo correto: CREATINA PREMIUM. Tente novamente. <<<\n");
                            } else {
                                prodNome[idDesejado] = nomeNovoProd;
                                System.out.print("PREÇO DE VENDA SUGERIDO (R$) [Exemplo correto: 49.90]: "); prodPrecos[idDesejado] = scanner.nextDouble();
                                prodPrecoOriginal[idDesejado] = prodPrecos[idDesejado];
                                System.out.print("ESTOQUE INICIAL [Exemplo correto: 50]: "); prodEstoque[idDesejado] = scanner.nextInt();
                                scanner.nextLine();
                                System.out.print("NCM [Exemplo correto: 2106.90.30]: "); prodNcm[idDesejado] = scanner.nextLine();

                                if (idDesejado >= totalProdutos) {
                                    totalProdutos = idDesejado + 1;
                                }
                                System.out.println(">>> PRODUTO CADASTRADO NO ID " + (idDesejado + 1) + "! <<<");
                            }
                        } else {
                            System.out.println(">>> ERRO: ID inválido! O valor deve estar entre 1 e 20.");
                            System.out.println(">>> O que é permitido: Números inteiros de 1 a 20.");
                            System.out.println(">>> Exemplo correto: 5. Tente novamente. <<<\n");
                        }
                    } else if (opcaoMenu == 4) {
                        if (totalProdutos > 0) {
                            System.out.println("\n--- AJUSTE DE PREÇOS E DESCONTOS ---");
                            for (int i = 0; i < totalProdutos; i++) {
                                if (prodNome[i] != null) {
                                    System.out.println("ID: " + (i + 1) + " | " + prodNome[i] + " | Preço Atual: " + formatarMoeda(prodPrecos[i]));
                                }
                            }
                            System.out.print("DIGITE O ID DO PRODUTO [Exemplo correto: 1]: ");
                            int idPr = -1;
                            try {
                                idPr = scanner.nextInt() - 1;
                            } catch (InputMismatchException e) {
                                scanner.nextLine();
                                System.out.println(">>> ERRO: ID inválido! <<<\n");
                                continue;
                            }

                            if (idPr >= 0 && idPr < totalProdutos && prodNome[idPr] != null) {
                                System.out.println("ESCOLHA O TIPO DE ALTERAÇÃO:");
                                System.out.println("1 - Definir novo preço de venda fixo");
                                System.out.println("2 - Aplicar porcentagem de desconto (%)");
                                System.out.print("OPÇÃO [Exemplo correto: 1]: ");
                                int tipoAlt = 1;
                                try {
                                    tipoAlt = scanner.nextInt();
                                } catch (InputMismatchException e) {
                                    scanner.nextLine();
                                    tipoAlt = 1;
                                }

                                if (tipoAlt == 1) {
                                    System.out.print("DIGITE O NOVO PREÇO DE VENDA (R$) [Exemplo correto: 89.90]: ");
                                    double novoPreco = scanner.nextDouble();
                                    if (novoPreco >= 0) {
                                        prodPrecos[idPr] = novoPreco;
                                        System.out.println(">>> PREÇO ATUALIZADO COM SUCESSO! <<<");
                                    } else {
                                        System.out.println(">>> ERRO: O preço não pode ser negativo!");
                                        System.out.println(">>> Exemplo correto: 89.90. Tente novamente. <<<\n");
                                    }
                                } else if (tipoAlt == 2) {
                                    System.out.print("DIGITE A PORCENTAGEM DE DESCONTO (%) [Exemplo correto: 15.0]: ");
                                    double desc = scanner.nextDouble();
                                    if (desc >= 0 && desc <= 100) {
                                        prodPrecos[idPr] -= (prodPrecos[idPr] * (desc / 100.0));
                                        System.out.println(">>> DESCONTO APLICADO COM SUCESSO! <<<");
                                    } else {
                                        System.out.println(">>> ERRO: Porcentagem inválida!");
                                        System.out.println(">>> O que é permitido: Valores entre 0 e 100.");
                                        System.out.println(">>> Exemplo correto: 15.0. Tente novamente. <<<\n");
                                    }
                                } else {
                                    System.out.println(">>> ERRO: Opção inválida!");
                                    System.out.println(">>> Exemplo correto: 1 ou 2. Tente novamente. <<<\n");
                                }
                            } else {
                                System.out.println(">>> ERRO: ID de produto inexistente no sistema!");
                                System.out.println(">>> Exemplo correto: Escolher um ID listado acima. Tente novamente. <<<\n");
                            }
                        } else {
                            System.out.println(">>> NENHUM PRODUTO CADASTRADO NO SISTEMA! <<<");
                        }
                    } else if (opcaoMenu == 5) {
                        if (totalProdutos > 0) {
                            System.out.println("\n--- MOVIMENTAÇÃO DE ESTOQUE E CUSTOS OPERACIONAIS ---");
                            for (int i = 0; i < totalProdutos; i++) {
                                if (prodNome[i] != null) {
                                    System.out.println("ID: " + (i + 1) + " | " + prodNome[i] + " | Estoque: " + prodEstoque[i]);
                                }
                            }
                            System.out.print("ID DO PRODUTO [Exemplo correto: 1]: ");
                            int posItem = -1;
                            try {
                                posItem = scanner.nextInt() - 1;
                            } catch (InputMismatchException e) {
                                scanner.nextLine();
                                continue;
                            }

                            if (posItem >= 0 && posItem < totalProdutos && prodNome[posItem] != null) {
                                System.out.print("QUANTIDADE ENTRADA [Exemplo correto: 30]: ");
                                int qtdeMover = scanner.nextInt();
                                System.out.print("CUSTO TOTAL DE ABASTECIMENTO (R$) [Exemplo correto: 1500.00]: ");
                                double custoAbastecimento = scanner.nextDouble();

                                if (saldoCaixaLoja >= custoAbastecimento) {
                                    saldoCaixaLoja -= custoAbastecimento; 
                                    totalGastosEstoque += custoAbastecimento; 
                                    prodEstoque[posItem] += qtdeMover;
                                    System.out.println(">>> ESTOQUE ATUALIZADO E DEDUZIDO DO CAIXA OPERACIONAL! <<<");
                                } else {
                                    System.out.println(">>> ERRO: Saldo em caixa insuficiente para cobrir este custo!");
                                    System.out.println(">>> Exemplo correto: Valor menor ou igual ao saldo atual do caixa. Tente novamente. <<<\n");
                                }
                            } else {
                                System.out.println(">>> ERRO: ID de produto inexistente!");
                                System.out.println(">>> Exemplo correto: Escolher um ID válido listado acima. Tente novamente. <<<\n");
                            }
                        }
                    } else if (opcaoMenu == 6) {
                        double folhaPendenteTotal = 0.0;
                        System.out.println("\n==================================================");
                        System.out.println("   DEMONSTRATIVO DA FOLHA SALARIAL E MÉDIA FISCAL ");
                        System.out.println("==================================================");
                        
                        for (int f = 0; f < totalFuncionarios; f++) {
                            if (funcNome[f] != null) {
                                String statusPagto = funcPago[f] ? "[JÁ PAGO]" : "[PENDENTE]";
                                String alertaMedia = verificarAlertaMediaSalarial(funcCargo[f], funcSalario[f]);
                                System.out.printf("ID: %s | %s (%s) - %s %s %s\n", 
                                        funcId[f], funcNome[f], funcCargo[f], formatarMoeda(funcSalario[f]), statusPagto, alertaMedia);
                                if (!funcPago[f]) {
                                    folhaPendenteTotal += funcSalario[f];
                                }
                            }
                        }
                        System.out.println("--------------------------------------------------");
                        System.out.println("VALOR TOTAL PENDENTE DE PAGAMENTO: " + formatarMoeda(folhaPendenteTotal));
                        System.out.println("(*) NOTA PARA O FINANCEIRO: Salários [ACIMA] ou [ABAIXO] da média exigem ");
                        System.out.println("    verificação de justificativa (Ex: comissões, horas extras ou tempo de casa).");

                        if (folhaPendenteTotal == 0) {
                            System.out.println("\n>>> TODOS OS FUNCIONÁRIOS JÁ FORAM PAGOS NESTE CICLO! <<<");
                        } else {
                            System.out.print("\nDESEJA QUITAR OS SALÁRIOS PENDENTES? [S/N] [Exemplo correto: S]: ");
                            String confPagto = scanner.next();

                            if (confPagto.equalsIgnoreCase("S")) {
                                if (saldoCaixaLoja >= folhaPendenteTotal) {
                                    saldoCaixaLoja -= folhaPendenteTotal;
                                    totalSalariosPagos += folhaPendenteTotal;
                                    
                                    for (int f = 0; f < totalFuncionarios; f++) {
                                        if (funcNome[f] != null && !funcPago[f]) {
                                            funcPago[f] = true;
                                        }
                                    }
                                    System.out.println(">>> FOLHA SALARIAL PENDENTE QUITADA COM SUCESSO! SALDO EM CAIXA ATUALIZADO. <<<");
                                } else {
                                    System.out.println(">>> ERRO: Saldo em caixa insuficiente para pagar os salários pendentes!");
                                    System.out.println(">>> Exemplo correto: Acumular receita em caixa antes de quitar a folha. Tente novamente. <<<\n");
                                }
                            }
                        }
                    } else if (opcaoMenu == 7) {
                        System.out.println("\n--- CONFIGURAÇÃO FISCAL E IMPOSTOS (RECEITA FEDERAL) ---");
                        System.out.println("ANO FISCAL ATUAL CONFIGURADO: " + anoExercicioPadrao);
                        System.out.print("DIGITE O NOVO ANO DESEJADO [Exemplo correto: 2027]: ");
                        try {
                            anoExercicioPadrao = scanner.nextInt();
                        } catch (InputMismatchException e) {
                            scanner.nextLine();
                            System.out.println(">>> ERRO: Ano inválido! Mantido o padrão. <<<\n");
                        }
                        System.out.println(">>> ANO FISCAL ALTERADO PARA " + anoExercicioPadrao + "! <<<");
                    } else if (opcaoMenu == 8) {
                        System.out.print("UF DESTINO [Exemplo correto: SP]: "); 
                        String ufDest = scanner.next().toUpperCase();
                        double totNota = 0, totImp = 0;
                        for (int i = 0; i < totalProdutos; i++) {
                            if (prodNome[i] != null) {
                                double valI = prodPrecos[i] * prodEstoque[i];
                                totNota += valI;
                                totImp += calcularImpostoItem(valI, buscarAliquotaIcms(prodNcm[i], ufOrigem, ufDest, anoExercicioPadrao));
                            }
                        }
                        System.out.println("\n--- NOTA FISCAL DE SAÍDA (EXERCÍCIO " + anoExercicioPadrao + ") ---");
                        System.out.println("VALOR BRUTO: " + formatarMoeda(totNota) + " | TRIBUTOS ESTIMADOS: " + formatarMoeda(totImp));
                    } else if (opcaoMenu == 9) {
                        double lucroBruto = totalReceitaVendas - totalGastosEstoque;
                        double lucroOperacional = lucroBruto - totalSalariosPagos;
                        double impostosSobreLucro = (lucroOperacional > 0) ? (lucroOperacional * 0.15) : 0.0;
                        double lucroLiquidoFinal = lucroOperacional - impostosSobreLucro;

                        System.out.println("\n==================================================");
                        System.out.println("      BALANÇO FINANCEIRO E DEMONSTRATIVO DRE      ");
                        System.out.println("==================================================");
                        System.out.println(" (★) SALDO ATUAL EM CAIXA DA LOJA: " + formatarMoeda(saldoCaixaLoja));
                        System.out.println(" (+) Receita Bruta de Vendas: " + formatarMoeda(totalReceitaVendas));
                        System.out.println(" (-) Custos de Abastecimento/Estoque: " + formatarMoeda(totalGastosEstoque));
                        System.out.println("--------------------------------------------------");
                        System.out.println(" (=) LUCRO BRUTO DA LOJA: " + formatarMoeda(lucroBruto));
                        System.out.println(" (-) Custos de Folha de Pagamento: " + formatarMoeda(totalSalariosPagos));
                        System.out.println("--------------------------------------------------");
                        System.out.println(" (=) Lucro Operacional Antes dos Impostos: " + formatarMoeda(lucroOperacional));
                        System.out.println(" (-) Impostos sobre o Lucro (IRPJ/CSLL ~15%): " + formatarMoeda(impostosSobreLucro));
                        System.out.println("--------------------------------------------------");
                        System.out.println(" (★) LUCRO LÍQUIDO FINAL DA LOJA: " + formatarMoeda(lucroLiquidoFinal));
                        System.out.println("==================================================");
                    } else if (opcaoMenu == 10) {
                        System.out.println("\n==================================================");
                        System.out.println("        LISTA DE FUNCIONÁRIOS CADASTRADOS         ");
                        System.out.println("==================================================");
                        for (int f = 0; f < totalFuncionarios; f++) {
                            if (funcNome[f] != null) {
                                String statusPg = funcPago[f] ? "Salário Pago" : "Salário Pendente";
                                String alertaMedia = verificarAlertaMediaSalarial(funcCargo[f], funcSalario[f]);
                                System.out.printf("ID: %s | NOME: %s | CARGO: %s | CPF: %s | SALÁRIO: %s | Status: %s %s\n", 
                                        funcId[f], funcNome[f], funcCargo[f], funcCpf[f], formatarMoeda(funcSalario[f]), statusPg, alertaMedia);
                            }
                        }
                        System.out.println("==================================================");
                    } else if (opcaoMenu == 11) {
                        System.out.println("\n--- ALTERAÇÃO DE SENHA ---");
                        System.out.print("DIGITE SUA SENHA ATUAL [Exemplo correto: admin123]: ");
                        String senhaAtual = scanner.nextLine();

                        if (funcSenha[funcLogadoIndex].equals(senhaAtual)) {
                            System.out.print("DIGITE A NOVA SENHA [Exemplo correto: novaSenha123]: ");
                            String novaSenha = scanner.nextLine().trim();
                            if (!novaSenha.isEmpty()) {
                                funcSenha[funcLogadoIndex] = novaSenha;
                                System.out.println(">>> SENHA ALTERADA COM SUCESSO! Utilize-a nos próximos acessos. <<<");
                            } else {
                                System.out.println(">>> ERRO: A nova senha não pode ser vazia!");
                                System.out.println(">>> Exemplo correto: senhaForte2026. Tente novamente. <<<\n");
                            }
                        } else {
                            System.out.println(">>> ERRO: Senha atual incorreta! A alteração foi cancelada.");
                            System.out.println(">>> Exemplo correto: Digitar a senha atual exata. Tente novamente. <<<\n");
                        }
                    } else if (opcaoMenu == 12) {
                        break; 
                    } else if (opcaoMenu == 13) {
                        sistemaAtivo = false;
                        break;
                    } else {
                        System.out.println(">>> ERRO: Opção de menu inexistente!");
                        System.out.println(">>> O que é permitido: Escolher entre 1 e 13.");
                        System.out.println(">>> Exemplo correto: 1. Tente novamente. <<<\n");
                    }
                }
            }
        }
        System.out.println("\n>>> SESSÃO FINALIZADA. ATÉ LOGO! <<<");
        scanner.close();
    }
}