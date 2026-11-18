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

public class prototo {

    /* #########################################################################
       #                                                                       #
       #   MÓDULO 1: MATRIZ FISCAL E REGRAS TRIBUTÁRIAS (2026 / 2027)          #
       #                                                                       #
       ######################################################################### */

    public static final double ALIQUOTA_ICMS_INTERNO_SP_2026 = 0.18;
    public static final double ALIQUOTA_ICMS_INTERNO_SP_2027 = 0.19;
    public static final double ALIQUOTA_ICMS_INTERESTADUAL_PADRAO = 0.12;

    public static double calcularAliquotaIcmsPorOperacao(String codigoNcmProduto, String ufOrigemOperacao, String ufDestinoOperacao, int anoExercicioFiscal) {
        double aliquotaInternaVigente = (anoExercicioFiscal >= 2027) ? ALIQUOTA_ICMS_INTERNO_SP_2027 : ALIQUOTA_ICMS_INTERNO_SP_2026;
        if (ufOrigemOperacao.equalsIgnoreCase("SP") && ufDestinoOperacao.equalsIgnoreCase("SP")) {
            return aliquotaInternaVigente;
        }
        return ALIQUOTA_ICMS_INTERESTADUAL_PADRAO;
    }

    public static double obterSalarioBasePorCargo(String cargoFuncionario) {
        if (cargoFuncionario.equalsIgnoreCase("GERENTE")) return 5500.00;
        if (cargoFuncionario.equalsIgnoreCase("FINANCEIRO")) return 4800.00;
        return 2200.00;
    }

    /* #########################################################################
       #                                                                       #
       #   MÓDULO 2: SEGURANÇA, CONTROLE DE ALÇADA (RBAC) E AUDITORIA          #
       #                                                                       #
       ######################################################################### */

    public static String gerarSenhaTemporariaAleatoria() {
        String conjuntoCaracteresPermitidos = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$";
        Random geradorAleatorio = new Random();
        StringBuilder senhaGerada = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            senhaGerada.append(conjuntoCaracteresPermitidos.charAt(geradorAleatorio.nextInt(conjuntoCaracteresPermitidos.length())));
        }
        return senhaGerada.toString();
    }

    public static String formatarValorMonetarioReais(double valorNumerico) {
        return String.format("R$ %.2f", valorNumerico);
    }

    public static void registrarEventoAuditoriaSistema(List<String> tabelaLogAuditoria, String nomeAutorAcao, String descricaoEvento) {
        String linhaRegistroAuditoria = "[AUDITORIA] Operador: " + nomeAutorAcao + " | Ação: " + descricaoEvento;
        tabelaLogAuditoria.add(linhaRegistroAuditoria);
    }

    public static boolean verificarUsuarioLogadoAindaPossuiAutoridade(
            String idUsuarioLogado, List<String> tabelaFuncId, List<String> tabelaFuncStatus) {
        int indice = tabelaFuncId.indexOf(idUsuarioLogado);
        if (indice == -1 || !tabelaFuncStatus.get(indice).equalsIgnoreCase("ATIVO")) {
            System.out.println("\n🚨 [REVOGAÇÃO DE SESSÃO]: Seu registro foi inativado ou excluído. Você não possui mais autoridade no sistema!\n");
            return false;
        }
        return true;
    }

    public static boolean validarPermissaoGerencialOuFinanceira(String cargoUsuarioAutenticado) {
        if (cargoUsuarioAutenticado.equalsIgnoreCase("GERENTE") || cargoUsuarioAutenticado.equalsIgnoreCase("FINANCEIRO")) {
            return true;
        }
        System.out.println("❌ [BLOQUEIO DE ALÇADA]: Acesso negado! Operação permitida apenas para GERENTE ou FINANCEIRO.\n");
        return false;
    }

    public static boolean validarPermissaoExclusivaGerente(String cargoUsuarioAutenticado) {
        if (cargoUsuarioAutenticado.equalsIgnoreCase("GERENTE")) {
            return true;
        }
        System.out.println("❌ [BLOQUEIO DE ALÇADA]: Operação restrita exclusivamente ao cargo de GERENTE.\n");
        return false;
    }

    /* #########################################################################
       #                                                                       #
       #   MÓDULO 3: VALIDAÇÕES DE ENTRADA, DOCUMENTOS E FORMATAÇÃO            #
       #                                                                       #
       ######################################################################### */

    public static String validarELerTextoAlfabetico(Scanner leitorEntrada, String mensagemPrompt) {
        while (true) {
            System.out.print(mensagemPrompt);
            String textoDigitado = leitorEntrada.nextLine().trim();
            if (textoDigitado.equals("0") || textoDigitado.equals("-0") || textoDigitado.equalsIgnoreCase("SAIR")) return "CANCELAR";
            if (!textoDigitado.isEmpty() && textoDigitado.matches("^[a-zA-ZáàâãéèêíïóôõúüçÁÀÂÃÉÈÊÍÏÓÔÕÚÜÇ\\s]+$")) {
                return textoDigitado.toUpperCase();
            }
            System.out.println("❌ [Validação]: Digite apenas letras e espaços (sem números, sinais ou campo vazio).\n");
        }
    }

    public static String validarELerTextoLivreObrigatorio(Scanner leitorEntrada, String mensagemPrompt) {
        while (true) {
            System.out.print(mensagemPrompt);
            String textoDigitado = leitorEntrada.nextLine().trim();
            if (textoDigitado.equals("0") || textoDigitado.equals("-0") || textoDigitado.equalsIgnoreCase("SAIR")) return "CANCELAR";
            if (!textoDigitado.isEmpty() && !textoDigitado.startsWith("-")) {
                return textoDigitado.toUpperCase();
            }
            System.out.println("❌ [Validação]: Campo obrigatório e não pode iniciar com '-' ou ficar vazio.\n");
        }
    }

    public static String validarELerSiglaEstadoUf(Scanner leitorEntrada, String mensagemPrompt) {
        List<String> listaSiglasEstadosBrasil = Arrays.asList(
            "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG",
            "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO"
        );
        while (true) {
            System.out.print(mensagemPrompt);
            String siglaUfDigitada = leitorEntrada.nextLine().trim().toUpperCase();
            if (siglaUfDigitada.equals("0") || siglaUfDigitada.equals("-0") || siglaUfDigitada.equalsIgnoreCase("SAIR")) return "CANCELAR";
            if (listaSiglasEstadosBrasil.contains(siglaUfDigitada)) return siglaUfDigitada;
            System.out.println("❌ [Validação]: Sigla de UF inválida. Exemplo válido: SP, RJ, MG.\n");
        }
    }

    public static int validarELerNumeroInteiro(Scanner leitorEntrada, String mensagemPrompt) {
        while (true) {
            System.out.print(mensagemPrompt);
            String entradaTexto = leitorEntrada.nextLine().trim();
            if (entradaTexto.equals("0") || entradaTexto.equalsIgnoreCase("SAIR")) return -999;
            if (entradaTexto.startsWith("-")) {
                System.out.println("❌ [Validação]: Valores negativos (como -0 ou números menores que zero) não são permitidos.\n");
                continue;
            }
            if (entradaTexto.matches("^[0-9]+$")) {
                try {
                    return Integer.parseInt(entradaTexto);
                } catch (NumberFormatException e) {
                    System.out.println("❌ [Validação]: Número inteiro excede o limite suportado.\n");
                }
            } else {
                System.out.println("❌ [Validação]: Digite apenas números inteiros positivos (ou 0 para voltar).\n");
            }
        }
    }

    public static int validarELerQuantidadePositiva(Scanner leitorEntrada, String mensagemPrompt) {
        while (true) {
            int valorLido = validarELerNumeroInteiro(leitorEntrada, mensagemPrompt);
            if (valorLido == -999) return -999;
            if (valorLido > 0) return valorLido;
            System.out.println("❌ [Validação]: A quantidade deve ser maior que zero.\n");
        }
    }

    public static double validarELerValorDecimalMonetario(Scanner leitorEntrada, String mensagemPrompt, boolean permitirZero) {
        while (true) {
            System.out.print(mensagemPrompt);
            String entradaTexto = leitorEntrada.nextLine().trim().replace(",", ".");
            if (entradaTexto.equalsIgnoreCase("SAIR") || (!permitirZero && entradaTexto.equals("0"))) return -999.0;
            if (entradaTexto.startsWith("-")) {
                System.out.println("❌ [Validação]: Valores negativos ou '-0' não são aceitos.\n");
                continue;
            }
            try {
                double valorConvertido = Double.parseDouble(entradaTexto);
                if (Double.isFinite(valorConvertido)) {
                    if (permitirZero && valorConvertido >= 0.0) return valorConvertido;
                    if (!permitirZero && valorConvertido > 0.0) return valorConvertido;
                }
            } catch (NumberFormatException ignored) {}
            System.out.println("❌ [Validação]: Informe um valor monetário válido maior que zero.\n");
        }
    }

    public static String validarEFormatarCpf(Scanner leitorEntrada) {
        while (true) {
            System.out.print("Digite o CPF (11 dígitos numéricos) [0 para cancelar]: ");
            String entradaTexto = leitorEntrada.nextLine().trim();
            if (entradaTexto.equals("0") || entradaTexto.equals("-0") || entradaTexto.equalsIgnoreCase("SAIR")) return "CANCELAR";
            if (entradaTexto.startsWith("-")) {
                System.out.println("❌ [Validação]: CPF não pode iniciar com sinal negativo.\n");
                continue;
            }
            String cpfSomenteNumeros = entradaTexto.replaceAll("[^0-9]", "");
            if (cpfSomenteNumeros.length() == 11) return cpfSomenteNumeros;
            System.out.println("❌ [Validação]: CPF inválido. Deve conter exatamente 11 dígitos.\n");
        }
    }

    public static String validarEFormatarCnpj(Scanner leitorEntrada) {
        while (true) {
            System.out.print("Digite o CNPJ (14 dígitos numéricos) [0 para cancelar]: ");
            String entradaTexto = leitorEntrada.nextLine().trim();
            if (entradaTexto.equals("0") || entradaTexto.equals("-0") || entradaTexto.equalsIgnoreCase("SAIR")) return "CANCELAR";
            if (entradaTexto.startsWith("-")) {
                System.out.println("❌ [Validação]: CNPJ não pode iniciar com sinal negativo.\n");
                continue;
            }
            String cnpjSomenteNumeros = entradaTexto.replaceAll("[^0-9]", "");
            if (cnpjSomenteNumeros.length() == 14) return cnpjSomenteNumeros;
            System.out.println("❌ [Validação]: CNPJ inválido. Deve conter exatamente 14 dígitos.\n");
        }
    }

    /* #########################################################################
       #                                                                       #
       #   MÓDULO 4: PERSISTÊNCIA, ROLLBACK PREVENTIVO E BACKUP EM ARQUIVO     #
       #                                                                       #
       ######################################################################### */

    public static void executarRollbackPreventivoAutomatico(
            List<String> tabelaFuncId, List<String> tabelaFuncNome, List<String> tabelaFuncCpf,
            List<String> tabelaFuncCargo, List<String> tabelaFuncSenha, List<Double> tabelaFuncSalario,
            List<Boolean> tabelaFuncPago, List<String> tabelaFuncStatus, List<String> tabelaFuncMotivoInativo,
            List<String> tabelaProdNome, List<String> tabelaProdNcm, List<Double> tabelaProdPrecoVenda,
            List<Integer> tabelaProdQtdEstoque, List<String> tabelaClienteCpf, List<String> tabelaClienteNome,
            List<Double> indicadorSaldoCaixaGlobal) {

        String nomeArquivoRollback = "protei_creat_rollback_auto.txt";
        try (BufferedWriter escritorArquivo = new BufferedWriter(new FileWriter(nomeArquivoRollback))) {
            escritorArquivo.write("#--- PONTO DE RESTAURAÇÃO AUTOMÁTICO (ROLLBACK) ---#\n");
            escritorArquivo.write("[FUNCIONARIOS_INICIO]\n");
            for (int i = 0; i < tabelaFuncId.size(); i++) {
                escritorArquivo.write(tabelaFuncId.get(i) + ";" + tabelaFuncNome.get(i) + ";" + tabelaFuncCpf.get(i) + ";" +
                                      tabelaFuncCargo.get(i) + ";" + tabelaFuncSenha.get(i) + ";" + tabelaFuncSalario.get(i) + ";" +
                                      tabelaFuncPago.get(i) + ";" + tabelaFuncStatus.get(i) + ";" + tabelaFuncMotivoInativo.get(i) + "\n");
            }
            escritorArquivo.write("[FUNCIONARIOS_FIM]\n");
            escritorArquivo.write("[PRODUTOS_INICIO]\n");
            for (int i = 0; i < tabelaProdNome.size(); i++) {
                if (tabelaProdNome.get(i) != null) {
                    escritorArquivo.write(tabelaProdNome.get(i) + ";" + tabelaProdNcm.get(i) + ";" +
                                          tabelaProdPrecoVenda.get(i) + ";" + tabelaProdQtdEstoque.get(i) + "\n");
                }
            }
            escritorArquivo.write("[PRODUTOS_FIM]\n");
            escritorArquivo.write("[CLIENTES_INICIO]\n");
            for (int i = 0; i < tabelaClienteCpf.size(); i++) {
                escritorArquivo.write(tabelaClienteCpf.get(i) + ";" + tabelaClienteNome.get(i) + "\n");
            }
            escritorArquivo.write("[CLIENTES_FIM]\n");
            escritorArquivo.write("[CAIXA_INICIO]\n" + indicadorSaldoCaixaGlobal.get(0) + "\n[CAIXA_FIM]\n");
        } catch (IOException ignored) {}
    }

    public static void exportarBackupOficialArquivo(
            List<String> tabelaFuncId, List<String> tabelaFuncNome, List<String> tabelaFuncCpf,
            List<String> tabelaFuncCargo, List<String> tabelaFuncSenha, List<Double> tabelaFuncSalario,
            List<Boolean> tabelaFuncPago, List<String> tabelaFuncStatus, List<String> tabelaFuncMotivoInativo,
            List<String> tabelaProdNome, List<String> tabelaProdNcm, List<Double> tabelaProdPrecoVenda,
            List<Integer> tabelaProdQtdEstoque, List<String> tabelaClienteCpf, List<String> tabelaClienteNome,
            List<Double> indicadorSaldoCaixaGlobal, List<String> tabelaTransacaoTipo, List<String> tabelaLogAuditoria,
            Scanner leitorEntrada, String nomeUsuarioLogado, String senhaUsuarioLogado) {

        System.out.println("\n================================================================");
        System.out.println("            TERMINAL: EXPORTAR BACKUP OFICIAL                   ");
        System.out.println("================================================================");
        System.out.printf("Resumo Atual -> Funcionários: %d | Produtos: %d | Clientes: %d | Transações: %d\n",
                tabelaFuncId.size(), tabelaProdNome.size(), tabelaClienteCpf.size(), tabelaTransacaoTipo.size());

        System.out.print("🔒 Confirme sua senha (" + nomeUsuarioLogado + ") [0 para cancelar]: ");
        String senhaConfirmacao = leitorEntrada.nextLine().trim();
        if (!senhaConfirmacao.equals(senhaUsuarioLogado)) {
            System.out.println("❌ Senha incorreta ou operação cancelada.\n");
            return;
        }

        String nomeArquivoBackup = "protei_creat_backup_oficial.txt";
        try (BufferedWriter escritorArquivo = new BufferedWriter(new FileWriter(nomeArquivoBackup))) {
            escritorArquivo.write("#--- BACKUP OFICIAL PROTEI CREAT ---#\n");
            escritorArquivo.write("[FUNCIONARIOS_INICIO]\n");
            for (int i = 0; i < tabelaFuncId.size(); i++) {
                escritorArquivo.write(tabelaFuncId.get(i) + ";" + tabelaFuncNome.get(i) + ";" + tabelaFuncCpf.get(i) + ";" +
                                      tabelaFuncCargo.get(i) + ";" + tabelaFuncSenha.get(i) + ";" + tabelaFuncSalario.get(i) + ";" +
                                      tabelaFuncPago.get(i) + ";" + tabelaFuncStatus.get(i) + ";" + tabelaFuncMotivoInativo.get(i) + "\n");
            }
            escritorArquivo.write("[FUNCIONARIOS_FIM]\n");
            escritorArquivo.write("[PRODUTOS_INICIO]\n");
            for (int i = 0; i < tabelaProdNome.size(); i++) {
                if (tabelaProdNome.get(i) != null) {
                    escritorArquivo.write(tabelaProdNome.get(i) + ";" + tabelaProdNcm.get(i) + ";" +
                                          tabelaProdPrecoVenda.get(i) + ";" + tabelaProdQtdEstoque.get(i) + "\n");
                }
            }
            escritorArquivo.write("[PRODUTOS_FIM]\n");
            escritorArquivo.write("[CLIENTES_INICIO]\n");
            for (int i = 0; i < tabelaClienteCpf.size(); i++) {
                escritorArquivo.write(tabelaClienteCpf.get(i) + ";" + tabelaClienteNome.get(i) + "\n");
            }
            escritorArquivo.write("[CLIENTES_FIM]\n");
            escritorArquivo.write("[CAIXA_INICIO]\n" + indicadorSaldoCaixaGlobal.get(0) + "\n[CAIXA_FIM]\n");

            registrarEventoAuditoriaSistema(tabelaLogAuditoria, nomeUsuarioLogado, "Exportou backup oficial em " + nomeArquivoBackup);
            System.out.println("✅ Backup oficial gerado com sucesso em '" + nomeArquivoBackup + "'!\n");
        } catch (IOException e) {
            System.out.println("❌ Falha técnica ao gravar arquivo de backup.\n");
        }
    }

    public static void importarRestaurarBackupArquivo(
            List<String> tabelaFuncId, List<String> tabelaFuncNome, List<String> tabelaFuncCpf,
            List<String> tabelaFuncCargo, List<String> tabelaFuncSenha, List<Double> tabelaFuncSalario,
            List<Boolean> tabelaFuncPago, List<String> tabelaFuncStatus, List<String> tabelaFuncMotivoInativo,
            List<String> tabelaProdNome, List<String> tabelaProdNcm, List<Double> tabelaProdPrecoVenda,
            List<Integer> tabelaProdQtdEstoque, List<String> tabelaClienteCpf, List<String> tabelaClienteNome,
            List<Double> indicadorSaldoCaixaGlobal, List<String> tabelaLogAuditoria, Scanner leitorEntrada,
            String cargoUsuarioLogado, String nomeUsuarioLogado, String senhaUsuarioLogado) {

        if (!validarPermissaoExclusivaGerente(cargoUsuarioLogado)) return;

        System.out.print("🔒 Confirme sua senha de GERENTE [0 para cancelar]: ");
        String senhaConfirmacao = leitorEntrada.nextLine().trim();
        if (!senhaConfirmacao.equals(senhaUsuarioLogado)) {
            System.out.println("❌ Senha incorreta.\n");
            return;
        }

        File arquivoBackup = new File("protei_creat_backup_oficial.txt");
        if (!arquivoBackup.exists()) {
            System.out.println("⚠️ Nenhum arquivo 'protei_creat_backup_oficial.txt' encontrado no diretório.\n");
            return;
        }

        executarRollbackPreventivoAutomatico(tabelaFuncId, tabelaFuncNome, tabelaFuncCpf, tabelaFuncCargo, tabelaFuncSenha,
                tabelaFuncSalario, tabelaFuncPago, tabelaFuncStatus, tabelaFuncMotivoInativo,
                tabelaProdNome, tabelaProdNcm, tabelaProdPrecoVenda, tabelaProdQtdEstoque,
                tabelaClienteCpf, tabelaClienteNome, indicadorSaldoCaixaGlobal);

        try (BufferedReader leitorArquivo = new BufferedReader(new FileReader(arquivoBackup))) {
            List<String> tempFuncId = new ArrayList<>(), tempFuncNome = new ArrayList<>(), tempFuncCpf = new ArrayList<>();
            List<String> tempFuncCargo = new ArrayList<>(), tempFuncSenha = new ArrayList<>(), tempFuncStatus = new ArrayList<>(), tempFuncMotivo = new ArrayList<>();
            List<Double> tempFuncSalario = new ArrayList<>();
            List<Boolean> tempFuncPago = new ArrayList<>();
            List<String> tempProdNome = new ArrayList<>(), tempProdNcm = new ArrayList<>();
            List<Double> tempProdPreco = new ArrayList<>();
            List<Integer> tempProdEstoque = new ArrayList<>();
            List<String> tempClienteCpf = new ArrayList<>(), tempClienteNome = new ArrayList<>();
            double tempCaixa = indicadorSaldoCaixaGlobal.get(0);

            String linhaAtual, blocoLeitura = "";
            while ((linhaAtual = leitorArquivo.readLine()) != null) {
                linhaAtual = linhaAtual.trim();
                if (linhaAtual.isEmpty() || linhaAtual.startsWith("#")) continue;
                if (linhaAtual.equals("[FUNCIONARIOS_INICIO]")) { blocoLeitura = "FUNC"; continue; }
                if (linhaAtual.equals("[FUNCIONARIOS_FIM]")) { blocoLeitura = ""; continue; }
                if (linhaAtual.equals("[PRODUTOS_INICIO]")) { blocoLeitura = "PROD"; continue; }
                if (linhaAtual.equals("[PRODUTOS_FIM]")) { blocoLeitura = ""; continue; }
                if (linhaAtual.equals("[CLIENTES_INICIO]")) { blocoLeitura = "CLIENTE"; continue; }
                if (linhaAtual.equals("[CLIENTES_FIM]")) { blocoLeitura = ""; continue; }
                if (linhaAtual.equals("[CAIXA_INICIO]")) { blocoLeitura = "CAIXA"; continue; }
                if (linhaAtual.equals("[CAIXA_FIM]")) { blocoLeitura = ""; continue; }

                if (blocoLeitura.equals("FUNC")) {
                    String[] colunas = linhaAtual.split(";");
                    tempFuncId.add(colunas[0]); tempFuncNome.add(colunas[1]); tempFuncCpf.add(colunas[2]);
                    tempFuncCargo.add(colunas[3]); tempFuncSenha.add(colunas[4]); tempFuncSalario.add(Double.parseDouble(colunas[5]));
                    tempFuncPago.add(Boolean.parseBoolean(colunas[6])); tempFuncStatus.add(colunas[7]); tempFuncMotivo.add(colunas[8]);
                } else if (blocoLeitura.equals("PROD")) {
                    String[] colunas = linhaAtual.split(";");
                    tempProdNome.add(colunas[0]); tempProdNcm.add(colunas[1]);
                    tempProdPreco.add(Double.parseDouble(colunas[2])); tempProdEstoque.add(Integer.parseInt(colunas[3]));
                } else if (blocoLeitura.equals("CLIENTE")) {
                    String[] colunas = linhaAtual.split(";");
                    tempClienteCpf.add(colunas[0]); tempClienteNome.add(colunas[1]);
                } else if (blocoLeitura.equals("CAIXA")) {
                    tempCaixa = Double.parseDouble(linhaAtual);
                }
            }

            tabelaFuncId.clear(); tabelaFuncId.addAll(tempFuncId);
            tabelaFuncNome.clear(); tabelaFuncNome.addAll(tempFuncNome);
            tabelaFuncCpf.clear(); tabelaFuncCpf.addAll(tempFuncCpf);
            tabelaFuncCargo.clear(); tabelaFuncCargo.addAll(tempFuncCargo);
            tabelaFuncSenha.clear(); tabelaFuncSenha.addAll(tempFuncSenha);
            tabelaFuncSalario.clear(); tabelaFuncSalario.addAll(tempFuncSalario);
            tabelaFuncPago.clear(); tabelaFuncPago.addAll(tempFuncPago);
            tabelaFuncStatus.clear(); tabelaFuncStatus.addAll(tempFuncStatus);
            tabelaFuncMotivoInativo.clear(); tabelaFuncMotivoInativo.addAll(tempFuncMotivo);

            tabelaProdNome.clear(); tabelaProdNome.addAll(tempProdNome);
            tabelaProdNcm.clear(); tabelaProdNcm.addAll(tempProdNcm);
            tabelaProdPrecoVenda.clear(); tabelaProdPrecoVenda.addAll(tempProdPreco);
            tabelaProdQtdEstoque.clear(); tabelaProdQtdEstoque.addAll(tempProdEstoque);

            tabelaClienteCpf.clear(); tabelaClienteCpf.addAll(tempClienteCpf);
            tabelaClienteNome.clear(); tabelaClienteNome.addAll(tempClienteNome);
            indicadorSaldoCaixaGlobal.set(0, tempCaixa);

            registrarEventoAuditoriaSistema(tabelaLogAuditoria, nomeUsuarioLogado, "Restaurou base a partir do backup oficial");
            System.out.println("✅ Restauração concluída com sucesso!\n");
        } catch (Exception e) {
            System.out.println("❌ Erro ao ler arquivo de backup.\n");
        }
    }

    /* #########################################################################
       #                                                                       #
       #   MÓDULO 5: CARGA DE TABELAS E MASSA DE DADOS EM MEMÓRIA              #
       #                                                                       #
       ######################################################################### */

    public static void inicializarTabelaFuncionarios(
            List<String> tabelaFuncId, List<String> tabelaFuncNome, List<String> tabelaFuncCpf,
            List<String> tabelaFuncCargo, List<String> tabelaFuncSenha, List<Double> tabelaFuncSalario,
            List<Boolean> tabelaFuncPago, List<String> tabelaFuncStatus, List<String> tabelaFuncMotivoInativo) {

        tabelaFuncId.clear(); tabelaFuncNome.clear(); tabelaFuncCpf.clear(); tabelaFuncCargo.clear();
        tabelaFuncSenha.clear(); tabelaFuncSalario.clear(); tabelaFuncPago.clear(); tabelaFuncStatus.clear(); tabelaFuncMotivoInativo.clear();

        tabelaFuncId.addAll(Arrays.asList("001", "002", "003", "004", "005", "006", "007", "008"));
        tabelaFuncNome.addAll(Arrays.asList("ANA SILVA", "BRUNO LIMA", "CAMILA SOUZA", "MARCOS OLIVEIRA", "JULIA MENDES", "ROBERTO ALVES", "PATRICIA LIMA", "LUCAS SANTIAGO"));
        tabelaFuncCpf.addAll(Arrays.asList("52998224725", "12345678909", "98765432100", "45678912300", "78912345600", "14725836900", "36925814700", "74185296300"));
        tabelaFuncCargo.addAll(Arrays.asList("GERENTE", "OPERADOR", "FINANCEIRO", "OPERADOR", "GERENTE", "OPERADOR", "FINANCEIRO", "OPERADOR"));
        tabelaFuncSenha.addAll(Arrays.asList("admin123", "op456", "fin789", "op999", "ger888", "op111", "fin222", "op333"));
        tabelaFuncSalario.addAll(Arrays.asList(5500.00, 2200.00, 4800.00, 2200.00, 5500.00, 2200.00, 4800.00, 2200.00));
        tabelaFuncPago.addAll(Arrays.asList(false, false, false, true, true, false, false, false));
        tabelaFuncStatus.addAll(Arrays.asList("ATIVO", "ATIVO", "ATIVO", "DEMITIDO", "DEMITIDO", "ATIVO", "ATIVO", "ATIVO"));
        tabelaFuncMotivoInativo.addAll(Arrays.asList("NENHUM", "NENHUM", "NENHUM", "Corte de custos", "Pedido de demissão", "NENHUM", "NENHUM", "NENHUM"));
    }

    public static void inicializarTabelaFornecedores(
            List<String> tabelaFornRazaoSocial, List<String> tabelaFornCnpj, List<String> tabelaFornUf,
            List<String> tabelaFornProdutoPrincipal, List<Double> tabelaFornValorFatura,
            List<Double> tabelaFornMargemLucroPct, List<Boolean> tabelaFornFaturaPaga) {

        tabelaFornRazaoSocial.clear(); tabelaFornCnpj.clear(); tabelaFornUf.clear();
        tabelaFornProdutoPrincipal.clear(); tabelaFornValorFatura.clear(); tabelaFornMargemLucroPct.clear(); tabelaFornFaturaPaga.clear();

        tabelaFornRazaoSocial.addAll(Arrays.asList("SUPLEMAX LTDA", "VITAFIT DISTRIBUIDORA", "GLOBAL NUTRI", "FARMACOMERCIAL BRASIL", "BIOHEDGER", "ATLAS SUPLEMENTOS"));
        tabelaFornCnpj.addAll(Arrays.asList("12345678000190", "98765432000110", "45678912000155", "78912345000188", "32165498000177", "65498732000144"));
        tabelaFornUf.addAll(Arrays.asList("SP", "RJ", "MG", "PR", "RS", "SC"));
        tabelaFornProdutoPrincipal.addAll(Arrays.asList("WHEY PROTEIN", "CREATINA", "BCAA 100G", "VITAMINAS", "GLUTAMINA", "BARRA PROTEICA"));
        tabelaFornValorFatura.addAll(Arrays.asList(4500.00, 3200.00, 2800.00, 1900.00, 3100.00, 1500.00));
        tabelaFornMargemLucroPct.addAll(Arrays.asList(40.0, 35.0, 40.0, 45.0, 35.0, 40.0));
        tabelaFornFaturaPaga.addAll(Arrays.asList(false, false, false, false, false, false));
    }

    public static void inicializarTabelaClientes(List<String> tabelaClienteCpf, List<String> tabelaClienteNome) {
        tabelaClienteCpf.clear(); tabelaClienteNome.clear();
        tabelaClienteCpf.addAll(Arrays.asList("11144477735", "22255588846", "33366699957", "44477700068", "55588811179", "66699922280"));
        tabelaClienteNome.addAll(Arrays.asList("JOAO PEREIRA", "MARIA ALMEIDA", "CARLOS EDUARDO", "BEATRIZ SOUZA", "LUCAS MARTINS", "JULIANA ROCHA"));
    }

    public static void popularBaseOperacional(
            boolean usarModoAleatorio,
            List<String> tabelaProdNome, List<String> tabelaProdNcm, List<Double> tabelaProdPrecoVenda, List<Integer> tabelaProdQtdEstoque,
            List<String> tabelaClienteNome,
            List<String> tabelaTransacaoTipo, List<String> tabelaTransacaoEnvolvidos, List<String> tabelaTransacaoDescricao, List<Double> tabelaTransacaoValor,
            List<Double> indicadorSaldoCaixaGlobal, List<Double> indicadorReceitaBrutaVendas,
            List<Double> indicadorCustoMercadoriaVendida, List<Double> indicadorTotalFornecedoresPagos,
            List<Double> indicadorTotalSalariosPagos, List<Double> indicadorImpostosProvisionados,
            int anoExercicioFiscal, List<String> tabelaLogAuditoria) {

        tabelaProdNome.clear(); tabelaProdNcm.clear(); tabelaProdPrecoVenda.clear(); tabelaProdQtdEstoque.clear();
        tabelaTransacaoTipo.clear(); tabelaTransacaoEnvolvidos.clear(); tabelaTransacaoDescricao.clear(); tabelaTransacaoValor.clear();

        Random gerador = usarModoAleatorio ? new Random() : new Random(777);
        String[] prefixosCategorias = {"WHEY", "CREATINA", "BCAA", "VITAMINA", "PRE-WORKOUT", "GLUTAMINA", "BARRA", "OMEGA"};
        int quantidadeProdutos = usarModoAleatorio ? (50 + gerador.nextInt(21)) : 55;

        for (int p = 1; p <= quantidadeProdutos; p++) {
            String categoria = prefixosCategorias[(p - 1) % prefixosCategorias.length];
            tabelaProdNome.add(categoria + (usarModoAleatorio ? " RANDOM " : " PADRÃO ") + p);
            tabelaProdNcm.add("2106.90." + (10 + (p % 89)));
            tabelaProdPrecoVenda.add(usarModoAleatorio ? (30.0 + gerador.nextDouble() * 150.0) : (35.0 + p * 2.0));
            tabelaProdQtdEstoque.add(usarModoAleatorio ? (10 + gerador.nextInt(90)) : (40 + (p % 40)));
        }

        double acumuladoReceitaVendas = 0.0;
        double acumuladoImpostos = 0.0;
        int quantidadeVendas = usarModoAleatorio ? (60 + gerador.nextInt(40)) : 100;

        for (int t = 1; t <= quantidadeVendas; t++) {
            int indiceProduto = gerador.nextInt(tabelaProdNome.size());
            int quantidadeItens = 1 + gerador.nextInt(4);
            double valorVenda = tabelaProdPrecoVenda.get(indiceProduto) * quantidadeItens;
            double aliquotaIcms = calcularAliquotaIcmsPorOperacao(tabelaProdNcm.get(indiceProduto), "SP", "SP", anoExercicioFiscal);
            String nomeCliente = tabelaClienteNome.get(gerador.nextInt(tabelaClienteNome.size()));

            tabelaTransacaoTipo.add("VENDA");
            tabelaTransacaoEnvolvidos.add("Cliente: " + nomeCliente + " | Caixa: TERMINAL 01");
            tabelaTransacaoDescricao.add(quantidadeItens + "x " + tabelaProdNome.get(indiceProduto));
            tabelaTransacaoValor.add(valorVenda);

            acumuladoReceitaVendas += valorVenda;
            acumuladoImpostos += (valorVenda * aliquotaIcms);
        }

        tabelaTransacaoTipo.add("COMPRA / ABASTECIMENTO");
        tabelaTransacaoEnvolvidos.add("Fornecedor: SUPLEMAX LTDA");
        tabelaTransacaoDescricao.add("Reposição inicial de lote WHEY PROTEIN");
        tabelaTransacaoValor.add(4500.00);

        tabelaTransacaoTipo.add("PAGAMENTO");
        tabelaTransacaoEnvolvidos.add("Funcionário: ANA SILVA (GERENTE)");
        tabelaTransacaoDescricao.add("Adiantamento de folha salarial");
        tabelaTransacaoValor.add(5500.00);

        indicadorReceitaBrutaVendas.set(0, acumuladoReceitaVendas);
        indicadorCustoMercadoriaVendida.set(0, acumuladoReceitaVendas * 0.45);
        indicadorTotalFornecedoresPagos.set(0, 4500.00);
        indicadorTotalSalariosPagos.set(0, 5500.00);
        indicadorImpostosProvisionados.set(0, acumuladoImpostos);
        indicadorSaldoCaixaGlobal.set(0, indicadorSaldoCaixaGlobal.get(0) + acumuladoReceitaVendas - 10000.00);

        String modoTexto = usarModoAleatorio ? "RANDÔMICA" : "PADRÃO";
        registrarEventoAuditoriaSistema(tabelaLogAuditoria, "SISTEMA", "Base operacional " + modoTexto + " inicializada.");
        System.out.println("✅ [Sucesso]: Base operacional " + modoTexto + " carregada com " + quantidadeProdutos + " produtos e " + tabelaTransacaoTipo.size() + " transações!\n");
    }

    /* #########################################################################
       #                                                                       #
       #   MÓDULO 6: GESTÃO DE RECURSOS HUMANOS E FOLHA DE PAGAMENTO           #
       #                                                                       #
       ######################################################################### */

    public static void exibirMenuGestaoFuncionarios(
            Scanner leitorEntrada,
            List<String> tabelaFuncId, List<String> tabelaFuncNome, List<String> tabelaFuncCpf,
            List<String> tabelaFuncCargo, List<String> tabelaFuncSenha, List<Double> tabelaFuncSalario,
            List<Boolean> tabelaFuncPago, List<String> tabelaFuncStatus, List<String> tabelaFuncMotivoInativo,
            List<String> tabelaLogAuditoria, String idUsuarioLogado, String nomeUsuarioLogado, String cargoUsuarioLogado,
            List<String> tabelaProdNome, List<String> tabelaProdNcm, List<Double> tabelaProdPrecoVenda, List<Integer> tabelaProdQtdEstoque,
            List<String> tabelaClienteCpf, List<String> tabelaClienteNome,
            List<Double> indicadorSaldoCaixaGlobal, List<Double> indicadorTotalSalariosPagos,
            List<String> tabelaTransacaoTipo, List<String> tabelaTransacaoEnvolvidos, List<String> tabelaTransacaoDescricao, List<Double> tabelaTransacaoValor) {

        boolean menuRhAtivo = true;
        while (menuRhAtivo) {
            if (!verificarUsuarioLogadoAindaPossuiAutoridade(idUsuarioLogado, tabelaFuncId, tabelaFuncStatus)) return;

            System.out.println("\n================================================================");
            System.out.println("        MÓDULO RH: GESTÃO DE AUTORIDADES E FUNCIONÁRIOS         ");
            System.out.println("================================================================");
            System.out.println("[1] Listar funcionários (Paginado 25/pág + Modal de Gestão/Pagamento)");
            System.out.println("[2] Cadastrar novo funcionário");
            System.out.println("[3] Voltar ao Painel Administrativo");
            System.out.println("================================================================");

            int opcaoEscolhida = validarELerNumeroInteiro(leitorEntrada, "Selecione a opção: ");
            if (opcaoEscolhida == 3 || opcaoEscolhida == -999) {
                menuRhAtivo = false;
            } else if (opcaoEscolhida == 1) {
                if (tabelaFuncNome.isEmpty()) {
                    System.out.println("⚠️ Nenhum funcionário cadastrado.\n");
                    continue;
                }
                int paginaAtual = 1;
                final int ITENS_POR_PAGINA = 25;
                boolean navegandoLista = true;

                while (navegandoLista) {
                    if (!verificarUsuarioLogadoAindaPossuiAutoridade(idUsuarioLogado, tabelaFuncId, tabelaFuncStatus)) return;

                    int totalPaginas = Math.max(1, (int) Math.ceil((double) tabelaFuncNome.size() / ITENS_POR_PAGINA));
                    if (paginaAtual > totalPaginas) paginaAtual = totalPaginas;

                    System.out.println("\n--- QUADRO DE FUNCIONÁRIOS (Página " + paginaAtual + " de " + totalPaginas + ") ---");
                    int indiceInicial = (paginaAtual - 1) * ITENS_POR_PAGINA;
                    int indiceFinal = Math.min(indiceInicial + ITENS_POR_PAGINA, tabelaFuncNome.size());

                    for (int i = indiceInicial; i < indiceFinal; i++) {
                        String etiquetaPagamento = tabelaFuncPago.get(i) ? "PAGO ✅" : "PENDENTE ⚠️";
                        String etiquetaStatus = tabelaFuncStatus.get(i).equalsIgnoreCase("ATIVO") ? "ATIVO" : "EXCLUÍDO/INATIVO (SEM AUTORIDADE) 🚫";
                        System.out.printf("[%d] ID: %s | Nome: %s | Cargo: %s | Status: %s | Salário: %s [%s]\n",
                                (i + 1), tabelaFuncId.get(i), tabelaFuncNome.get(i), tabelaFuncCargo.get(i),
                                etiquetaStatus, formatarValorMonetarioReais(tabelaFuncSalario.get(i)), etiquetaPagamento);
                    }
                    System.out.println("----------------------------------------------------------------");
                    System.out.println("[N] Próxima Página | [P] Página Anterior | [NÚMERO] Abrir Modal | [0] Voltar");
                    System.out.print("Comando: ");
                    String comandoUsuario = leitorEntrada.nextLine().trim().toUpperCase();

                    if (comandoUsuario.equals("N") && paginaAtual < totalPaginas) paginaAtual++;
                    else if (comandoUsuario.equals("P") && paginaAtual > 1) paginaAtual--;
                    else if (comandoUsuario.equals("0") || comandoUsuario.equals("-0") || comandoUsuario.equals("SAIR")) navegandoLista = false;
                    else if (comandoUsuario.matches("^[0-9]+$")) {
                        int indiceSelecionado = Integer.parseInt(comandoUsuario) - 1;
                        if (indiceSelecionado >= 0 && indiceSelecionado < tabelaFuncNome.size()) {
                            abrirModalDetalhesEGestaoFuncionario(
                                    leitorEntrada, indiceSelecionado,
                                    tabelaFuncId, tabelaFuncNome, tabelaFuncCpf, tabelaFuncCargo, tabelaFuncSenha, tabelaFuncSalario,
                                    tabelaFuncPago, tabelaFuncStatus, tabelaFuncMotivoInativo,
                                    indicadorSaldoCaixaGlobal, indicadorTotalSalariosPagos,
                                    tabelaTransacaoTipo, tabelaTransacaoEnvolvidos, tabelaTransacaoDescricao, tabelaTransacaoValor,
                                    tabelaLogAuditoria, idUsuarioLogado, nomeUsuarioLogado, cargoUsuarioLogado
                            );
                        } else {
                            System.out.println("❌ Índice inválido.\n");
                        }
                    } else {
                        System.out.println("❌ Comando inválido.\n");
                    }
                }
            } else if (opcaoEscolhida == 2) {
                if (!validarPermissaoExclusivaGerente(cargoUsuarioLogado)) continue;

                executarRollbackPreventivoAutomatico(tabelaFuncId, tabelaFuncNome, tabelaFuncCpf, tabelaFuncCargo, tabelaFuncSenha,
                        tabelaFuncSalario, tabelaFuncPago, tabelaFuncStatus, tabelaFuncMotivoInativo,
                        tabelaProdNome, tabelaProdNcm, tabelaProdPrecoVenda, tabelaProdQtdEstoque,
                        tabelaClienteCpf, tabelaClienteNome, indicadorSaldoCaixaGlobal);

                String novoIdFuncional = "";
                while (true) {
                    System.out.print("Novo ID Funcional (3 dígitos, ex: 009) [0 para cancelar]: ");
                    novoIdFuncional = leitorEntrada.nextLine().trim().toUpperCase();
                    if (novoIdFuncional.equals("0") || novoIdFuncional.equals("-0") || novoIdFuncional.equalsIgnoreCase("SAIR")) break;
                    if (novoIdFuncional.matches("^[0-9]{3}$") && !tabelaFuncId.contains(novoIdFuncional)) break;
                    System.out.println("❌ ID inválido (use 3 dígitos numéricos) ou já cadastrado.\n");
                }
                if (novoIdFuncional.equals("0") || novoIdFuncional.equals("-0") || novoIdFuncional.equalsIgnoreCase("SAIR")) continue;

                String nomeCompleto = validarELerTextoAlfabetico(leitorEntrada, "Nome completo do colaborador: ");
                if (nomeCompleto.equals("CANCELAR")) continue;
                String cpfValidado = validarEFormatarCpf(leitorEntrada);
                if (cpfValidado.equals("CANCELAR")) continue;

                int opcaoCargo = validarELerNumeroInteiro(leitorEntrada, "Cargo [1 - GERENTE | 2 - OPERADOR | 3 - FINANCEIRO] [0 para cancelar]: ");
                if (opcaoCargo == -999) continue;
                if (opcaoCargo < 1 || opcaoCargo > 3) {
                    System.out.println("❌ Opção de cargo inválida.\n");
                    continue;
                }
                String cargoDefinido = (opcaoCargo == 1) ? "GERENTE" : (opcaoCargo == 3 ? "FINANCEIRO" : "OPERADOR");
                String senhaGerada = gerarSenhaTemporariaAleatoria();

                tabelaFuncId.add(novoIdFuncional);
                tabelaFuncNome.add(nomeCompleto);
                tabelaFuncCpf.add(cpfValidado);
                tabelaFuncCargo.add(cargoDefinido);
                tabelaFuncSenha.add(senhaGerada);
                tabelaFuncSalario.add(obterSalarioBasePorCargo(cargoDefinido));
                tabelaFuncPago.add(false);
                tabelaFuncStatus.add("ATIVO");
                tabelaFuncMotivoInativo.add("NENHUM");

                registrarEventoAuditoriaSistema(tabelaLogAuditoria, nomeUsuarioLogado, "Cadastrou funcionário: " + nomeCompleto + " (" + cargoDefinido + ")");
                System.out.println("✅ Funcionário cadastrado! Senha inicial gerada: " + senhaGerada + "\n");
            } else {
                System.out.println("❌ Opção inválida.\n");
            }
        }
    }

    private static void abrirModalDetalhesEGestaoFuncionario(
            Scanner leitorEntrada, int indiceFuncionario,
            List<String> tabelaFuncId, List<String> tabelaFuncNome, List<String> tabelaFuncCpf,
            List<String> tabelaFuncCargo, List<String> tabelaFuncSenha, List<Double> tabelaFuncSalario,
            List<Boolean> tabelaFuncPago, List<String> tabelaFuncStatus, List<String> tabelaFuncMotivoInativo,
            List<Double> indicadorSaldoCaixaGlobal, List<Double> indicadorTotalSalariosPagos,
            List<String> tabelaTransacaoTipo, List<String> tabelaTransacaoEnvolvidos, List<String> tabelaTransacaoDescricao, List<Double> tabelaTransacaoValor,
            List<String> tabelaLogAuditoria, String idUsuarioLogado, String nomeUsuarioLogado, String cargoUsuarioLogado) {

        boolean modalAberto = true;
        while (modalAberto) {
            boolean estaAtivo = tabelaFuncStatus.get(indiceFuncionario).equalsIgnoreCase("ATIVO");

            System.out.println("\n┌────────────────────────────────────────────────────────┐");
            System.out.println("│               [MODAL DE GESTÃO DE FUNCIONÁRIO]         │");
            System.out.println("├────────────────────────────────────────────────────────┤");
            System.out.printf("│ Nome: %s | ID: %s | CPF: %s\n", tabelaFuncNome.get(indiceFuncionario), tabelaFuncId.get(indiceFuncionario), tabelaFuncCpf.get(indiceFuncionario));
            System.out.printf("│ Cargo: %s | Status: %s\n", tabelaFuncCargo.get(indiceFuncionario), (estaAtivo ? "ATIVO" : "EXCLUÍDO / DEMITIDO"));
            System.out.printf("│ Autoridade no Sistema: %s\n", (estaAtivo ? "HABILITADA (" + tabelaFuncCargo.get(indiceFuncionario) + ")" : "REVOGADA (SEM ACESSO)"));
            System.out.printf("│ Salário: %s | Situação Folha: %s\n", formatarValorMonetarioReais(tabelaFuncSalario.get(indiceFuncionario)), (tabelaFuncPago.get(indiceFuncionario) ? "PAGO" : "PENDENTE"));
            if (!estaAtivo) {
                System.out.printf("│ Motivo Desligamento: %s\n", tabelaFuncMotivoInativo.get(indiceFuncionario));
            }
            System.out.println("├────────────────────────────────────────────────────────┤");
            System.out.println("│ [1] Ver Ficha Técnica Completa                         │");
            System.out.println("│ [2] Efetuar Pagamento de Salário (Se pendente)         │");
            System.out.println("│ [3] Editar Nome do Colaborador (Exclusivo Gerente)     │");
            System.out.println("│ [4] Demitir / Revogar Autoridade (Exclusivo Gerente)   │");
            System.out.println("│ [0] Fechar Modal                                       │");
            System.out.println("└────────────────────────────────────────────────────────┘");

            int acaoEscolhida = validarELerNumeroInteiro(leitorEntrada, "Ação no Modal: ");
            if (acaoEscolhida == 1) {
                System.out.println("\n--- FICHA DETALHADA DO COLABORADOR ---");
                System.out.println("• Nome Completo      : " + tabelaFuncNome.get(indiceFuncionario));
                System.out.println("• Matrícula ID       : " + tabelaFuncId.get(indiceFuncionario));
                System.out.println("• CPF                : " + tabelaFuncCpf.get(indiceFuncionario));
                System.out.println("• Cargo              : " + tabelaFuncCargo.get(indiceFuncionario));
                System.out.println("• Status do Registro : " + (estaAtivo ? "ATIVO" : "EXCLUÍDO / INATIVO"));
                System.out.println("• Autoridade         : " + (estaAtivo ? "ATIVA" : "NENHUMA (REVOGADA)"));
                System.out.println("• Remuneração        : " + formatarValorMonetarioReais(tabelaFuncSalario.get(indiceFuncionario)));
                System.out.println("----------------------------------------------------------------\n");
            } else if (acaoEscolhida == 2) {
                if (!validarPermissaoGerencialOuFinanceira(cargoUsuarioLogado)) continue;
                if (!estaAtivo) {
                    System.out.println("🚫 [REGISTRO EXCLUÍDO]: Este funcionário já está inativo/demitido e não possui mais vínculo ou autoridade.\n");
                } else if (tabelaFuncPago.get(indiceFuncionario)) {
                    System.out.println("⚠️ O salário deste funcionário já foi quitado neste ciclo!\n");
                } else {
                    double valorSalario = tabelaFuncSalario.get(indiceFuncionario);
                    if (indicadorSaldoCaixaGlobal.get(0) < valorSalario) {
                        System.out.println("❌ Saldo em caixa insuficiente para liquidar a folha!\n");
                        continue;
                    }
                    indicadorSaldoCaixaGlobal.set(0, indicadorSaldoCaixaGlobal.get(0) - valorSalario);
                    indicadorTotalSalariosPagos.set(0, indicadorTotalSalariosPagos.get(0) + valorSalario);
                    tabelaFuncPago.set(indiceFuncionario, true);

                    tabelaTransacaoTipo.add("PAGAMENTO");
                    tabelaTransacaoEnvolvidos.add("Funcionário: " + tabelaFuncNome.get(indiceFuncionario));
                    tabelaTransacaoDescricao.add("Quitação de Salário Mensal");
                    tabelaTransacaoValor.add(valorSalario);

                    registrarEventoAuditoriaSistema(tabelaLogAuditoria, nomeUsuarioLogado, "Quitou salário de " + tabelaFuncNome.get(indiceFuncionario));
                    System.out.println("✅ Salário pago com sucesso! Caixa e histórico financeiro atualizados.\n");
                }
            } else if (acaoEscolhida == 3) {
                if (!validarPermissaoExclusivaGerente(cargoUsuarioLogado)) continue;
                if (!estaAtivo) {
                    System.out.println("🚫 [REGISTRO EXCLUÍDO]: Não é possível editar um colaborador já excluído/inativo.\n");
                    continue;
                }
                String novoNome = validarELerTextoAlfabetico(leitorEntrada, "Novo nome completo: ");
                if (!novoNome.equals("CANCELAR")) {
                    tabelaFuncNome.set(indiceFuncionario, novoNome);
                    registrarEventoAuditoriaSistema(tabelaLogAuditoria, nomeUsuarioLogado, "Editou nome do ID " + tabelaFuncId.get(indiceFuncionario));
                    System.out.println("✅ Nome atualizado!\n");
                }
            } else if (acaoEscolhida == 4) {
                if (!validarPermissaoExclusivaGerente(cargoUsuarioLogado)) continue;
                if (!estaAtivo) {
                    System.out.println("⚠️ Este funcionário JÁ ESTÁ EXCLUÍDO/INATIVO e sem autoridade no sistema!\n");
                    continue;
                }
                String motivoDesligamento = validarELerTextoLivreObrigatorio(leitorEntrada, "Motivo do desligamento [0 para cancelar]: ");
                if (motivoDesligamento.equals("CANCELAR")) continue;

                tabelaFuncStatus.set(indiceFuncionario, "DEMITIDO");
                tabelaFuncCargo.set(indiceFuncionario, "SEM CARGO (REVOGADO)");
                tabelaFuncSenha.set(indiceFuncionario, "BLOQUEADA_" + gerarSenhaTemporariaAleatoria());
                tabelaFuncMotivoInativo.set(indiceFuncionario, motivoDesligamento);

                registrarEventoAuditoriaSistema(tabelaLogAuditoria, nomeUsuarioLogado, "Excluiu/inativou " + tabelaFuncNome.get(indiceFuncionario) + " e revogou sua autoridade");
                System.out.println("✅ Funcionário excluído/inativado com sucesso! Credenciais bloqueadas e autoridade 100% revogada.\n");

                if (tabelaFuncId.get(indiceFuncionario).equals(idUsuarioLogado)) {
                    System.out.println("🚨 Você revogou seu próprio acesso. Encerrando sessão imediatamente...\n");
                    return;
                }
                modalAberto = false;
            } else if (acaoEscolhida == 0 || acaoEscolhida == -999) {
                modalAberto = false;
            } else {
                System.out.println("❌ Opção inválida.\n");
            }
        }
    }

    /* #########################################################################
       #                                                                       #
       #   MÓDULO 7: GESTÃO DE FORNECEDORES E ABASTECIMENTO DE ESTOQUE         #
       #                                                                       #
       ######################################################################### */

    public static void exibirMenuGestaoFornecedores(
            Scanner leitorEntrada,
            List<String> tabelaFornRazaoSocial, List<String> tabelaFornCnpj, List<String> tabelaFornUf,
            List<String> tabelaFornProdutoPrincipal, List<Double> tabelaFornValorFatura,
            List<Double> tabelaFornMargemLucroPct, List<Boolean> tabelaFornFaturaPaga,
            List<String> tabelaProdNome, List<String> tabelaProdNcm, List<Double> tabelaProdPrecoVenda, List<Integer> tabelaProdQtdEstoque,
            List<Double> indicadorSaldoCaixaGlobal, List<Double> indicadorGastosEstoque, List<Double> indicadorTotalFornecedoresPagos,
            List<String> tabelaTransacaoTipo, List<String> tabelaTransacaoEnvolvidos, List<String> tabelaTransacaoDescricao, List<Double> tabelaTransacaoValor,
            List<String> tabelaLogAuditoria, String nomeUsuarioLogado, String cargoUsuarioLogado) {

        boolean menuFornecedoresAtivo = true;
        while (menuFornecedoresAtivo) {
            System.out.println("\n================================================================");
            System.out.println("             MÓDULO SUPRIMENTOS: GESTÃO DE FORNECEDORES         ");
            System.out.println("================================================================");
            System.out.println("[1] Listar fornecedores (Paginado 25/pág + Modal de Detalhes/Pagamento)");
            System.out.println("[2] Cadastrar fornecedor e efetuar compra de abastecimento");
            System.out.println("[3] Voltar ao Painel Administrativo");
            System.out.println("================================================================");

            int opcaoEscolhida = validarELerNumeroInteiro(leitorEntrada, "Selecione a opção: ");
            if (opcaoEscolhida == 3 || opcaoEscolhida == -999) {
                menuFornecedoresAtivo = false;
            } else if (opcaoEscolhida == 1) {
                if (tabelaFornRazaoSocial.isEmpty()) {
                    System.out.println("⚠️ Nenhum fornecedor ativo na base (todos foram excluídos ou ainda não há cadastros).\n");
                    continue;
                }
                int paginaAtual = 1;
                final int ITENS_POR_PAGINA = 25;
                boolean navegandoLista = true;

                while (navegandoLista) {
                    if (tabelaFornRazaoSocial.isEmpty()) {
                        System.out.println("ℹ️ Todos os fornecedores foram excluídos. Retornando ao menu...\n");
                        break;
                    }
                    int totalPaginas = Math.max(1, (int) Math.ceil((double) tabelaFornRazaoSocial.size() / ITENS_POR_PAGINA));
                    if (paginaAtual > totalPaginas) paginaAtual = totalPaginas;

                    System.out.println("\n--- CARTEIRA DE FORNECEDORES (Página " + paginaAtual + " de " + totalPaginas + ") ---");
                    int indiceInicial = (paginaAtual - 1) * ITENS_POR_PAGINA;
                    int indiceFinal = Math.min(indiceInicial + ITENS_POR_PAGINA, tabelaFornRazaoSocial.size());

                    for (int i = indiceInicial; i < indiceFinal; i++) {
                        String etiquetaFatura = tabelaFornFaturaPaga.get(i) ? "QUITADO ✅" : "PENDENTE ⚠️";
                        System.out.printf("[%d] %s | CNPJ: %s | UF: %s | Fatura: %s [%s]\n",
                                (i + 1), tabelaFornRazaoSocial.get(i), tabelaFornCnpj.get(i), tabelaFornUf.get(i),
                                formatarValorMonetarioReais(tabelaFornValorFatura.get(i)), etiquetaFatura);
                    }
                    System.out.println("----------------------------------------------------------------");
                    System.out.println("[N] Próxima Página | [P] Página Anterior | [NÚMERO] Abrir Modal | [0] Voltar");
                    System.out.print("Comando: ");
                    String comandoUsuario = leitorEntrada.nextLine().trim().toUpperCase();

                    if (comandoUsuario.equals("N") && paginaAtual < totalPaginas) paginaAtual++;
                    else if (comandoUsuario.equals("P") && paginaAtual > 1) paginaAtual--;
                    else if (comandoUsuario.equals("0") || comandoUsuario.equals("-0") || comandoUsuario.equals("SAIR")) navegandoLista = false;
                    else if (comandoUsuario.matches("^[0-9]+$")) {
                        int indiceSelecionado = Integer.parseInt(comandoUsuario) - 1;
                        if (indiceSelecionado >= 0 && indiceSelecionado < tabelaFornRazaoSocial.size()) {
                            abrirModalDetalhesEGestaoFornecedor(
                                    leitorEntrada, indiceSelecionado,
                                    tabelaFornRazaoSocial, tabelaFornCnpj, tabelaFornUf, tabelaFornProdutoPrincipal,
                                    tabelaFornValorFatura, tabelaFornMargemLucroPct, tabelaFornFaturaPaga,
                                    indicadorSaldoCaixaGlobal, indicadorTotalFornecedoresPagos,
                                    tabelaTransacaoTipo, tabelaTransacaoEnvolvidos, tabelaTransacaoDescricao, tabelaTransacaoValor,
                                    tabelaLogAuditoria, nomeUsuarioLogado, cargoUsuarioLogado
                            );
                        } else {
                            System.out.println("❌ Índice inválido ou já excluído.\n");
                        }
                    } else {
                        System.out.println("❌ Comando inválido.\n");
                    }
                }
            } else if (opcaoEscolhida == 2) {
                if (!validarPermissaoGerencialOuFinanceira(cargoUsuarioLogado)) continue;

                String razaoSocial = validarELerTextoAlfabetico(leitorEntrada, "Razão Social do Fornecedor: ");
                if (razaoSocial.equals("CANCELAR")) continue;
                String cnpjValidado = validarEFormatarCnpj(leitorEntrada);
                if (cnpjValidado.equals("CANCELAR")) continue;
                String siglaUf = validarELerSiglaEstadoUf(leitorEntrada, "Estado de origem (UF): ");
                if (siglaUf.equals("CANCELAR")) continue;

                String nomeProduto = validarELerTextoLivreObrigatorio(leitorEntrada, "Nome do produto fornecido [0 para cancelar]: ");
                if (nomeProduto.equals("CANCELAR")) continue;

                double custoUnitario = validarELerValorDecimalMonetario(leitorEntrada, "Custo unitário de compra (R$) [0 para cancelar]: ", false);
                if (custoUnitario <= 0) continue;

                int quantidadeCompra = validarELerQuantidadePositiva(leitorEntrada, "Quantidade de unidades compradas [0 para cancelar]: ");
                if (quantidadeCompra <= 0) continue;

                double valorTotalFatura = custoUnitario * quantidadeCompra;
                if (indicadorSaldoCaixaGlobal.get(0) < valorTotalFatura) {
                    System.out.println("❌ Saldo em caixa insuficiente para concluir a compra!\n");
                    continue;
                }

                tabelaFornRazaoSocial.add(razaoSocial);
                tabelaFornCnpj.add(cnpjValidado);
                tabelaFornUf.add(siglaUf);
                tabelaFornProdutoPrincipal.add(nomeProduto);
                tabelaFornValorFatura.add(valorTotalFatura);
                tabelaFornMargemLucroPct.add(40.0);
                tabelaFornFaturaPaga.add(true);

                indicadorSaldoCaixaGlobal.set(0, indicadorSaldoCaixaGlobal.get(0) - valorTotalFatura);
                indicadorGastosEstoque.set(0, indicadorGastosEstoque.get(0) + valorTotalFatura);
                indicadorTotalFornecedoresPagos.set(0, indicadorTotalFornecedoresPagos.get(0) + valorTotalFatura);

                int indiceProdutoExistente = tabelaProdNome.indexOf(nomeProduto);
                if (indiceProdutoExistente != -1) {
                    tabelaProdQtdEstoque.set(indiceProdutoExistente, tabelaProdQtdEstoque.get(indiceProdutoExistente) + quantidadeCompra);
                } else {
                    tabelaProdNome.add(nomeProduto);
                    tabelaProdNcm.add("2106.90.30");
                    tabelaProdPrecoVenda.add(custoUnitario * 1.40);
                    tabelaProdQtdEstoque.add(quantidadeCompra);
                }

                tabelaTransacaoTipo.add("COMPRA / ABASTECIMENTO");
                tabelaTransacaoEnvolvidos.add("Fornecedor: " + razaoSocial + " (" + siglaUf + ")");
                tabelaTransacaoDescricao.add("Entrada em estoque: " + quantidadeCompra + "x " + nomeProduto);
                tabelaTransacaoValor.add(valorTotalFatura);

                registrarEventoAuditoriaSistema(tabelaLogAuditoria, nomeUsuarioLogado, "Compra registrada junto a " + razaoSocial);
                System.out.println("✅ Compra registrada! Estoque, caixa e histórico atualizados.\n");
            } else {
                System.out.println("❌ Opção inválida.\n");
            }
        }
    }

    private static void abrirModalDetalhesEGestaoFornecedor(
            Scanner leitorEntrada, int indiceFornecedor,
            List<String> tabelaFornRazaoSocial, List<String> tabelaFornCnpj, List<String> tabelaFornUf,
            List<String> tabelaFornProdutoPrincipal, List<Double> tabelaFornValorFatura,
            List<Double> tabelaFornMargemLucroPct, List<Boolean> tabelaFornFaturaPaga,
            List<Double> indicadorSaldoCaixaGlobal, List<Double> indicadorTotalFornecedoresPagos,
            List<String> tabelaTransacaoTipo, List<String> tabelaTransacaoEnvolvidos, List<String> tabelaTransacaoDescricao, List<Double> tabelaTransacaoValor,
            List<String> tabelaLogAuditoria, String nomeUsuarioLogado, String cargoUsuarioLogado) {

        boolean modalAberto = true;
        while (modalAberto) {
            if (indiceFornecedor < 0 || indiceFornecedor >= tabelaFornRazaoSocial.size()) {
                System.out.println("⚠️ Este fornecedor já foi excluído do sistema.\n");
                return;
            }

            System.out.println("\n┌────────────────────────────────────────────────────────┐");
            System.out.println("│               [MODAL DE GESTÃO DE FORNECEDOR]          │");
            System.out.println("├────────────────────────────────────────────────────────┤");
            System.out.printf("│ Razão Social: %s\n", tabelaFornRazaoSocial.get(indiceFornecedor));
            System.out.printf("│ CNPJ: %s | UF: %s | Produto: %s\n", tabelaFornCnpj.get(indiceFornecedor), tabelaFornUf.get(indiceFornecedor), tabelaFornProdutoPrincipal.get(indiceFornecedor));
            System.out.printf("│ Fatura: %s | Situação: %s\n", formatarValorMonetarioReais(tabelaFornValorFatura.get(indiceFornecedor)), (tabelaFornFaturaPaga.get(indiceFornecedor) ? "QUITADO" : "PENDENTE"));
            System.out.println("├────────────────────────────────────────────────────────┤");
            System.out.println("│ [1] Ver Detalhes Comerciais                            │");
            System.out.println("│ [2] Quitar Fatura Pendente (Se não paga)               │");
            System.out.println("│ [3] Editar Razão Social                                │");
            System.out.println("│ [4] Excluir Fornecedor (Exclusivo Gerente)             │");
            System.out.println("│ [0] Fechar Modal                                       │");
            System.out.println("└────────────────────────────────────────────────────────┘");

            int acaoEscolhida = validarELerNumeroInteiro(leitorEntrada, "Ação no Modal: ");
            if (acaoEscolhida == 1) {
                System.out.println("\n--- FICHA COMERCIAL DO FORNECEDOR ---");
                System.out.println("• Razão Social   : " + tabelaFornRazaoSocial.get(indiceFornecedor));
                System.out.println("• CNPJ           : " + tabelaFornCnpj.get(indiceFornecedor));
                System.out.println("• Estado (UF)    : " + tabelaFornUf.get(indiceFornecedor));
                System.out.println("• Item Fornecido : " + tabelaFornProdutoPrincipal.get(indiceFornecedor));
                System.out.println("• Valor Fatura   : " + formatarValorMonetarioReais(tabelaFornValorFatura.get(indiceFornecedor)));
                System.out.println("• Markup Padrão  : " + tabelaFornMargemLucroPct.get(indiceFornecedor) + "%");
                System.out.println("----------------------------------------------------------------\n");
            } else if (acaoEscolhida == 2) {
                if (!validarPermissaoGerencialOuFinanceira(cargoUsuarioLogado)) continue;
                if (tabelaFornFaturaPaga.get(indiceFornecedor)) {
                    System.out.println("⚠️ Esta fatura já consta como QUITADA!\n");
                } else {
                    double valorFatura = tabelaFornValorFatura.get(indiceFornecedor);
                    if (indicadorSaldoCaixaGlobal.get(0) < valorFatura) {
                        System.out.println("❌ Saldo em caixa insuficiente!\n");
                        continue;
                    }
                    indicadorSaldoCaixaGlobal.set(0, indicadorSaldoCaixaGlobal.get(0) - valorFatura);
                    indicadorTotalFornecedoresPagos.set(0, indicadorTotalFornecedoresPagos.get(0) + valorFatura);
                    tabelaFornFaturaPaga.set(indiceFornecedor, true);

                    tabelaTransacaoTipo.add("PAGAMENTO");
                    tabelaTransacaoEnvolvidos.add("Fornecedor: " + tabelaFornRazaoSocial.get(indiceFornecedor));
                    tabelaTransacaoDescricao.add("Quitação de fatura (" + tabelaFornProdutoPrincipal.get(indiceFornecedor) + ")");
                    tabelaTransacaoValor.add(valorFatura);

                    registrarEventoAuditoriaSistema(tabelaLogAuditoria, nomeUsuarioLogado, "Pagou fatura de " + tabelaFornRazaoSocial.get(indiceFornecedor));
                    System.out.println("✅ Fatura quitada com sucesso! Caixa e transações atualizados.\n");
                }
            } else if (acaoEscolhida == 3) {
                if (!validarPermissaoGerencialOuFinanceira(cargoUsuarioLogado)) continue;
                String novaRazaoSocial = validarELerTextoAlfabetico(leitorEntrada, "Nova Razão Social: ");
                if (!novaRazaoSocial.equals("CANCELAR")) {
                    tabelaFornRazaoSocial.set(indiceFornecedor, novaRazaoSocial);
                    System.out.println("✅ Razão social atualizada!\n");
                }
            } else if (acaoEscolhida == 4) {
                if (!validarPermissaoExclusivaGerente(cargoUsuarioLogado)) continue;
                String nomeRemovido = tabelaFornRazaoSocial.get(indiceFornecedor);
                tabelaFornRazaoSocial.remove(indiceFornecedor);
                tabelaFornCnpj.remove(indiceFornecedor);
                tabelaFornUf.remove(indiceFornecedor);
                tabelaFornProdutoPrincipal.remove(indiceFornecedor);
                tabelaFornValorFatura.remove(indiceFornecedor);
                tabelaFornMargemLucroPct.remove(indiceFornecedor);
                tabelaFornFaturaPaga.remove(indiceFornecedor);
                registrarEventoAuditoriaSistema(tabelaLogAuditoria, nomeUsuarioLogado, "Excluiu fornecedor: " + nomeRemovido);
                System.out.println("🗑️ Fornecedor '" + nomeRemovido + "' EXCLUÍDO com sucesso! Registro removido e sem vínculo operacional.\n");
                modalAberto = false;
            } else if (acaoEscolhida == 0 || acaoEscolhida == -999) {
                modalAberto = false;
            } else {
                System.out.println("❌ Opção inválida.\n");
            }
        }
    }

    /* #########################################################################
       #                                                                       #
       #   MÓDULO 8: PAINEL FINANCEIRO, ESTOQUE E HISTÓRICO DE TRANSAÇÕES      #
       #                                                                       #
       ######################################################################### */

    public static void exibirRelatorioVolumeComprasClientes(
            Scanner leitorEntrada, List<String> tabelaTransacaoTipo,
            List<String> tabelaTransacaoEnvolvidos, List<String> tabelaTransacaoDescricao, List<Double> tabelaTransacaoValor,
            List<String> tabelaClienteCpf, List<String> tabelaClienteNome,
            List<String> tabelaLogAuditoria, String nomeUsuarioLogado) {

        System.out.println("\n================================================================");
        System.out.println("       RELATÓRIO GERENCIAL: VOLUME DE COMPRAS POR CLIENTE       ");
        System.out.println("================================================================");

        System.out.print("Deseja filtrar por transação específica ou palavra-chave (produto/termo)? [S/N]: ");
        String opcaoFiltro = leitorEntrada.nextLine().trim().toUpperCase();
        String termoFiltro = "";
        if (opcaoFiltro.equals("S")) {
            termoFiltro = validarELerTextoLivreObrigatorio(leitorEntrada, "Digite o termo para filtrar (ex: WHEY, CREATINA, TERMINAL) [0 para cancelar]: ");
            if (termoFiltro.equals("CANCELAR")) termoFiltro = "";
        }

        List<String> nomesClientesUnicos = new ArrayList<>();
        List<Double> totaisComprasClientes = new ArrayList<>();
        List<Integer> quantidadePedidosClientes = new ArrayList<>();

        double totalGeralVendas = 0.0;
        int totalPedidosGeral = 0;

        for (int i = 0; i < tabelaTransacaoTipo.size(); i++) {
            if (tabelaTransacaoTipo.get(i).equalsIgnoreCase("VENDA")) {
                String envolvidos = tabelaTransacaoEnvolvidos.get(i);
                String descricao = tabelaTransacaoDescricao.get(i);
                double valor = tabelaTransacaoValor.get(i);

                if (!termoFiltro.isEmpty()) {
                    if (!envolvidos.toUpperCase().contains(termoFiltro) && !descricao.toUpperCase().contains(termoFiltro)) {
                        continue;
                    }
                }

                String nomeCliente = "CLIENTE AVULSO";
                if (envolvidos.contains("Cliente:")) {
                    int inicio = envolvidos.indexOf("Cliente:") + 8;
                    int fim = envolvidos.indexOf(" (CPF:");
                    if (fim == -1) fim = envolvidos.indexOf(" |");
                    if (fim == -1) fim = envolvidos.length();
                    nomeCliente = envolvidos.substring(inicio, fim).trim().toUpperCase();
                }

                totalGeralVendas += valor;
                totalPedidosGeral++;

                int idx = nomesClientesUnicos.indexOf(nomeCliente);
                if (idx != -1) {
                    totaisComprasClientes.set(idx, totaisComprasClientes.get(idx) + valor);
                    quantidadePedidosClientes.set(idx, quantidadePedidosClientes.get(idx) + 1);
                } else {
                    nomesClientesUnicos.add(nomeCliente);
                    totaisComprasClientes.add(valor);
                    quantidadePedidosClientes.add(1);
                }
            }
        }

        String etiquetaStatusFiltro = termoFiltro.isEmpty() ? "TODAS AS TRANSAÇÕES" : "FILTRO ATIVO: '" + termoFiltro + "'";
        System.out.println("\n--- RESULTADO DO RELATÓRIO [" + etiquetaStatusFiltro + "] ---");

        if (nomesClientesUnicos.isEmpty()) {
            System.out.println("⚠️ Nenhuma venda encontrada com os critérios informados.\n");
            System.out.print("Pressione ENTER para retornar: ");
            leitorEntrada.nextLine();
            return;
        }

        System.out.printf("%-24s | %-8s | %-15s | %-15s\n", "CLIENTE", "PEDIDOS", "VOLUME TOTAL", "TICKET MÉDIO");
        System.out.println("----------------------------------------------------------------------------------");
        for (int i = 0; i < nomesClientesUnicos.size(); i++) {
            double ticketMedioCliente = totaisComprasClientes.get(i) / quantidadePedidosClientes.get(i);
            System.out.printf("%-24s | %8d | %15s | %15s\n",
                    nomesClientesUnicos.get(i),
                    quantidadePedidosClientes.get(i),
                    formatarValorMonetarioReais(totaisComprasClientes.get(i)),
                    formatarValorMonetarioReais(ticketMedioCliente));
        }
        System.out.println("----------------------------------------------------------------------------------");
        double ticketMedioGeral = (totalPedidosGeral > 0) ? (totalGeralVendas / totalPedidosGeral) : 0.0;
        System.out.printf("TOTAIS: %d pedidos | Volume: %s | Ticket Médio Geral: %s\n",
                totalPedidosGeral, formatarValorMonetarioReais(totalGeralVendas), formatarValorMonetarioReais(ticketMedioGeral));
        System.out.println("==================================================================================\n");

        System.out.print("Deseja exportar este relatório para um arquivo de texto (.txt)? [S/N]: ");
        String opcaoExportar = leitorEntrada.nextLine().trim().toUpperCase();
        if (opcaoExportar.equals("S")) {
            String nomeArquivoRelatorio = "relatorio_volume_compras_clientes.txt";
            try (BufferedWriter escritorArquivo = new BufferedWriter(new FileWriter(nomeArquivoRelatorio))) {
                escritorArquivo.write("==================================================================================\n");
                escritorArquivo.write("       RELATÓRIO GERENCIAL: VOLUME DE COMPRAS POR CLIENTE       \n");
                escritorArquivo.write("==================================================================================\n");
                escritorArquivo.write("Filtro Aplicado: " + (termoFiltro.isEmpty() ? "NENHUM (TODAS AS TRANSAÇÕES)" : termoFiltro) + "\n");
                escritorArquivo.write("----------------------------------------------------------------------------------\n");
                escritorArquivo.write(String.format("%-24s | %-8s | %-15s | %-15s\n", "CLIENTE", "PEDIDOS", "VOLUME TOTAL", "TICKET MÉDIO"));
                escritorArquivo.write("----------------------------------------------------------------------------------\n");
                for (int i = 0; i < nomesClientesUnicos.size(); i++) {
                    double ticketMedioCliente = totaisComprasClientes.get(i) / quantidadePedidosClientes.get(i);
                    escritorArquivo.write(String.format("%-24s | %8d | %15s | %15s\n",
                            nomesClientesUnicos.get(i),
                            quantidadePedidosClientes.get(i),
                            formatarValorMonetarioReais(totaisComprasClientes.get(i)),
                            formatarValorMonetarioReais(ticketMedioCliente)));
                }
                escritorArquivo.write("----------------------------------------------------------------------------------\n");
                escritorArquivo.write(String.format("TOTAIS: %d pedidos | Volume: %s | Ticket Médio Geral: %s\n",
                        totalPedidosGeral, formatarValorMonetarioReais(totalGeralVendas), formatarValorMonetarioReais(ticketMedioGeral)));
                escritorArquivo.write("==================================================================================\n");

                registrarEventoAuditoriaSistema(tabelaLogAuditoria, nomeUsuarioLogado, "Exportou relatório de volume e ticket médio de compras para " + nomeArquivoRelatorio);
                System.out.println("✅ Relatório exportado com sucesso para '" + nomeArquivoRelatorio + "'!\n");
            } catch (IOException e) {
                System.out.println("❌ Erro ao gravar o arquivo de relatório.\n");
            }
        }

        System.out.print("Pressione ENTER para retornar ao menu financeiro: ");
        leitorEntrada.nextLine();
    }

    public static void exibirMenuFinanceiroEstoqueEHistorico(
            Scanner leitorEntrada,
            List<String> tabelaProdNome, List<String> tabelaProdNcm, List<Double> tabelaProdPrecoVenda, List<Integer> tabelaProdQtdEstoque,
            List<String> tabelaClienteCpf, List<String> tabelaClienteNome,
            List<Double> indicadorSaldoCaixaGlobal, List<Double> indicadorGastosEstoque, List<Double> indicadorReceitaBrutaVendas,
            List<Double> indicadorCustoMercadoriaVendida, List<Double> indicadorTotalSalariosPagos,
            List<Double> indicadorTotalFornecedoresPagos, List<Double> indicadorImpostosProvisionados,
            List<String> tabelaTransacaoTipo, List<String> tabelaTransacaoEnvolvidos, List<String> tabelaTransacaoDescricao, List<Double> tabelaTransacaoValor,
            List<String> tabelaFuncId, List<String> tabelaFuncNome, List<String> tabelaFuncCpf, List<String> tabelaFuncCargo,
            List<String> tabelaFuncSenha, List<Double> tabelaFuncSalario, List<Boolean> tabelaFuncPago, List<String> tabelaFuncStatus, List<String> tabelaFuncMotivoInativo,
            List<String> tabelaLogAuditoria, String idUsuarioLogado, String nomeUsuarioLogado, String cargoUsuarioLogado, String senhaUsuarioLogado) {

        boolean menuFinanceiroAtivo = true;
        while (menuFinanceiroAtivo) {
            if (!verificarUsuarioLogadoAindaPossuiAutoridade(idUsuarioLogado, tabelaFuncId, tabelaFuncStatus)) return;

            System.out.println("\n================================================================");
            System.out.println("      MÓDULO FINANCEIRO, ESTOQUE E AUDITORIA (" + cargoUsuarioLogado + ")");
            System.out.println("================================================================");
            System.out.println("[1] Catálogo de Produtos e Estoque (Paginado 25/pág + Filtro + Modal)");
            System.out.println("[2] Cadastrar Novo Produto no Catálogo");
            System.out.println("[3] Histórico Financeiro e Transações (Paginado 25/pág + Filtro + Modal)");
            System.out.println("[4] Relatório Gerencial: Volume de Compras por Cliente (Com Exportação TXT)");
            System.out.println("[5] Demonstrativo Financeiro Consolidado (DRE Resumida)");
            System.out.println("[6] Consultar Log de Auditoria do Sistema (Paginado 25/pág + Filtro)");
            System.out.println("[7] [TERMINAL] Exportar Backup Oficial");
            System.out.println("[8] [TERMINAL] Restaurar Backup Oficial (Exclusivo Gerente)");
            System.out.println("[9] Voltar ao Painel Administrativo");
            System.out.println("================================================================");

            int opcaoEscolhida = validarELerNumeroInteiro(leitorEntrada, "Selecione a opção: ");
            if (opcaoEscolhida == 9 || opcaoEscolhida == -999) {
                menuFinanceiroAtivo = false;
            } else if (opcaoEscolhida == 1) {
                gerenciarCatalogoProdutosPaginado(leitorEntrada, tabelaProdNome, tabelaProdNcm, tabelaProdPrecoVenda, tabelaProdQtdEstoque, tabelaLogAuditoria, nomeUsuarioLogado, cargoUsuarioLogado);
            } else if (opcaoEscolhida == 2) {
                if (!validarPermissaoGerencialOuFinanceira(cargoUsuarioLogado)) continue;
                executarRollbackPreventivoAutomatico(tabelaFuncId, tabelaFuncNome, tabelaFuncCpf, tabelaFuncCargo, tabelaFuncSenha,
                        tabelaFuncSalario, tabelaFuncPago, tabelaFuncStatus, tabelaFuncMotivoInativo,
                        tabelaProdNome, tabelaProdNcm, tabelaProdPrecoVenda, tabelaProdQtdEstoque,
                        tabelaClienteCpf, tabelaClienteNome, indicadorSaldoCaixaGlobal);

                String nomeProduto = validarELerTextoLivreObrigatorio(leitorEntrada, "Nome do novo produto [0 para cancelar]: ");
                if (nomeProduto.equals("CANCELAR")) continue;
                double precoVenda = validarELerValorDecimalMonetario(leitorEntrada, "Preço unitário de venda (R$) [0 para cancelar]: ", false);
                if (precoVenda <= 0) continue;
                int estoqueInicial = validarELerNumeroInteiro(leitorEntrada, "Quantidade inicial em estoque [0 para cancelar]: ");
                if (estoqueInicial == -999) continue;

                tabelaProdNome.add(nomeProduto);
                tabelaProdNcm.add("2106.90.30");
                tabelaProdPrecoVenda.add(precoVenda);
                tabelaProdQtdEstoque.add(estoqueInicial);

                registrarEventoAuditoriaSistema(tabelaLogAuditoria, nomeUsuarioLogado, "Cadastrou produto: " + nomeProduto);
                System.out.println("✅ Produto adicionado ao catálogo!\n");
            } else if (opcaoEscolhida == 3) {
                gerenciarHistoricoTransacoesPaginado(leitorEntrada, tabelaTransacaoTipo, tabelaTransacaoEnvolvidos, tabelaTransacaoDescricao, tabelaTransacaoValor, indicadorSaldoCaixaGlobal, tabelaLogAuditoria, nomeUsuarioLogado, cargoUsuarioLogado);
            } else if (opcaoEscolhida == 4) {
                exibirRelatorioVolumeComprasClientes(leitorEntrada, tabelaTransacaoTipo, tabelaTransacaoEnvolvidos, tabelaTransacaoDescricao, tabelaTransacaoValor, tabelaClienteCpf, tabelaClienteNome, tabelaLogAuditoria, nomeUsuarioLogado);
            } else if (opcaoEscolhida == 5) {
                System.out.println("\n================ DEMONSTRATIVO FINANCEIRO CONSOLIDADO ================");
                System.out.println("• Saldo Atual em Caixa Global      : " + formatarValorMonetarioReais(indicadorSaldoCaixaGlobal.get(0)));
                System.out.println("• Receita Bruta de Vendas          : " + formatarValorMonetarioReais(indicadorReceitaBrutaVendas.get(0)));
                System.out.println("• Custo Mercadoria Vendida (CMV)   : " + formatarValorMonetarioReais(indicadorCustoMercadoriaVendida.get(0)));
                System.out.println("• Investimento / Gastos Estoque    : " + formatarValorMonetarioReais(indicadorGastosEstoque.get(0)));
                System.out.println("• Folha de Pagamento Quitada       : " + formatarValorMonetarioReais(indicadorTotalSalariosPagos.get(0)));
                System.out.println("• Fornecedores Quitados            : " + formatarValorMonetarioReais(indicadorTotalFornecedoresPagos.get(0)));
                System.out.println("• Impostos Provisionados (ICMS)    : " + formatarValorMonetarioReais(indicadorImpostosProvisionados.get(0)));
                System.out.println("======================================================================\n");
            } else if (opcaoEscolhida == 6) {
                exibirLogAuditoriaPaginado(leitorEntrada, tabelaLogAuditoria, cargoUsuarioLogado);
            } else if (opcaoEscolhida == 7) {
                exportarBackupOficialArquivo(tabelaFuncId, tabelaFuncNome, tabelaFuncCpf, tabelaFuncCargo, tabelaFuncSenha,
                        tabelaFuncSalario, tabelaFuncPago, tabelaFuncStatus, tabelaFuncMotivoInativo,
                        tabelaProdNome, tabelaProdNcm, tabelaProdPrecoVenda, tabelaProdQtdEstoque,
                        tabelaClienteCpf, tabelaClienteNome, indicadorSaldoCaixaGlobal,
                        tabelaTransacaoTipo, tabelaLogAuditoria, leitorEntrada, nomeUsuarioLogado, senhaUsuarioLogado);
            } else if (opcaoEscolhida == 8) {
                importarRestaurarBackupArquivo(tabelaFuncId, tabelaFuncNome, tabelaFuncCpf, tabelaFuncCargo, tabelaFuncSenha,
                        tabelaFuncSalario, tabelaFuncPago, tabelaFuncStatus, tabelaFuncMotivoInativo,
                        tabelaProdNome, tabelaProdNcm, tabelaProdPrecoVenda, tabelaProdQtdEstoque,
                        tabelaClienteCpf, tabelaClienteNome, indicadorSaldoCaixaGlobal,
                        tabelaLogAuditoria, leitorEntrada, cargoUsuarioLogado, nomeUsuarioLogado, senhaUsuarioLogado);
            } else {
                System.out.println("❌ Opção inválida.\n");
            }
        }
    }

    private static void exibirLogAuditoriaPaginado(Scanner leitorEntrada, List<String> tabelaLogAuditoria, String cargoUsuarioLogado) {
        if (!validarPermissaoExclusivaGerente(cargoUsuarioLogado)) return;

        if (tabelaLogAuditoria.isEmpty()) {
            System.out.println("⚠️ Nenhum registro encontrado no Log de Auditoria.\n");
            return;
        }

        int paginaAtual = 1;
        final int ITENS_POR_PAGINA = 25;
        String filtroAtivo = "";
        boolean navegandoLog = true;

        while (navegandoLog) {
            List<String> listaRegistrosFiltrados = new ArrayList<>();
            for (String evento : tabelaLogAuditoria) {
                if (filtroAtivo.isEmpty() || evento.toUpperCase().contains(filtroAtivo)) {
                    listaRegistrosFiltrados.add(evento);
                }
            }

            int totalRegistros = listaRegistrosFiltrados.size();
            int totalPaginas = Math.max(1, (int) Math.ceil((double) totalRegistros / ITENS_POR_PAGINA));
            if (paginaAtual > totalPaginas) paginaAtual = totalPaginas;

            String etiquetaFiltro = filtroAtivo.isEmpty() ? "SEM FILTRO" : "FILTRO: '" + filtroAtivo + "'";
            System.out.println("\n--- LOG DE AUDITORIA DO SISTEMA [" + etiquetaFiltro + "] (Página " + paginaAtual + " de " + totalPaginas + " | Encontrados: " + totalRegistros + ") ---");

            if (totalRegistros == 0) {
                System.out.println("⚠️ [ALERTA DE FILTRO]: Nenhum evento de auditoria corresponde ao termo '" + filtroAtivo + "'. Pressione [L] para limpar o filtro.");
            } else {
                int indiceInicial = (paginaAtual - 1) * ITENS_POR_PAGINA;
                int indiceFinal = Math.min(indiceInicial + ITENS_POR_PAGINA, totalRegistros);
                for (int i = indiceInicial; i < indiceFinal; i++) {
                    System.out.printf("[%d] %s\n", (i + 1), listaRegistrosFiltrados.get(i));
                }
            }

            System.out.println("----------------------------------------------------------------");
            System.out.println("[N] Próxima Página | [P] Página Anterior | [F] Filtrar por Palavra-Chave/Operador | [L] Limpar Filtro | [0] Voltar");
            System.out.print("Comando: ");
            String comandoUsuario = leitorEntrada.nextLine().trim().toUpperCase();

            if (comandoUsuario.equals("N") && paginaAtual < totalPaginas) {
                paginaAtual++;
            } else if (comandoUsuario.equals("P") && paginaAtual > 1) {
                paginaAtual--;
            } else if (comandoUsuario.equals("F")) {
                String termoBusca = validarELerTextoLivreObrigatorio(leitorEntrada, "Digite o operador ou palavra-chave para filtrar [0 para cancelar]: ");
                if (!termoBusca.equals("CANCELAR")) {
                    filtroAtivo = termoBusca;
                    paginaAtual = 1;
                }
            } else if (comandoUsuario.equals("L")) {
                filtroAtivo = "";
                paginaAtual = 1;
                System.out.println("✅ Filtro removido. Exibindo todos os eventos.\n");
            } else if (comandoUsuario.equals("0") || comandoUsuario.equals("-0") || comandoUsuario.equals("SAIR")) {
                navegandoLog = false;
            } else {
                System.out.println("❌ Comando inválido.\n");
            }
        }
    }

    private static void gerenciarCatalogoProdutosPaginado(
            Scanner leitorEntrada, List<String> tabelaProdNome, List<String> tabelaProdNcm,
            List<Double> tabelaProdPrecoVenda, List<Integer> tabelaProdQtdEstoque,
            List<String> tabelaLogAuditoria, String nomeUsuarioLogado, String cargoUsuarioLogado) {

        if (tabelaProdNome.isEmpty()) {
            System.out.println("⚠️ Catálogo de produtos vazio.\n");
            return;
        }

        int paginaAtual = 1;
        final int ITENS_POR_PAGINA = 25;
        String filtroProdutoAtivo = "";
        boolean navegandoLista = true;

        while (navegandoLista) {
            if (tabelaProdNome.isEmpty()) {
                System.out.println("ℹ️ Todos os produtos foram excluídos do catálogo.\n");
                break;
            }

            List<Integer> listaIndicesReaisFiltrados = new ArrayList<>();
            for (int i = 0; i < tabelaProdNome.size(); i++) {
                String nome = tabelaProdNome.get(i);
                String ncm = tabelaProdNcm.get(i);
                if (nome != null) {
                    if (filtroProdutoAtivo.isEmpty() ||
                        nome.toUpperCase().contains(filtroProdutoAtivo) ||
                        ncm.toUpperCase().contains(filtroProdutoAtivo)) {
                        listaIndicesReaisFiltrados.add(i);
                    }
                }
            }

            int totalFiltrados = listaIndicesReaisFiltrados.size();
            int totalPaginas = Math.max(1, (int) Math.ceil((double) totalFiltrados / ITENS_POR_PAGINA));
            if (paginaAtual > totalPaginas) paginaAtual = totalPaginas;

            String etiquetaFiltro = filtroProdutoAtivo.isEmpty() ? "TODOS OS ITENS" : "FILTRO: '" + filtroProdutoAtivo + "'";
            System.out.println("\n--- CATÁLOGO DE PRODUTOS [" + etiquetaFiltro + "] (Página " + paginaAtual + " de " + totalPaginas + " | Encontrados: " + totalFiltrados + ") ---");

            if (totalFiltrados == 0) {
                System.out.println("⚠️ [ALERTA DE FILTRO]: Nenhum produto encontrado para o filtro '" + filtroProdutoAtivo + "'. Pressione [L] para limpar o filtro.");
            } else {
                int indiceInicial = (paginaAtual - 1) * ITENS_POR_PAGINA;
                int indiceFinal = Math.min(indiceInicial + ITENS_POR_PAGINA, totalFiltrados);

                for (int i = indiceInicial; i < indiceFinal; i++) {
                    int indiceReal = listaIndicesReaisFiltrados.get(i);
                    System.out.printf("[%d] %s (NCM: %s) | Preço: %s | Estoque: %d un\n",
                            (i + 1), tabelaProdNome.get(indiceReal), tabelaProdNcm.get(indiceReal),
                            formatarValorMonetarioReais(tabelaProdPrecoVenda.get(indiceReal)), tabelaProdQtdEstoque.get(indiceReal));
                }
            }

            System.out.println("----------------------------------------------------------------");
            System.out.println("[N] Próxima | [P] Anterior | [F] Filtrar Nome/NCM | [L] Limpar Filtro | [NÚMERO] Abrir Modal | [0] Voltar");
            System.out.print("Comando: ");
            String comandoUsuario = leitorEntrada.nextLine().trim().toUpperCase();

            if (comandoUsuario.equals("N") && paginaAtual < totalPaginas) {
                paginaAtual++;
            } else if (comandoUsuario.equals("P") && paginaAtual > 1) {
                paginaAtual--;
            } else if (comandoUsuario.equals("F")) {
                String termoBusca = validarELerTextoLivreObrigatorio(leitorEntrada, "Digite o nome do produto ou NCM para filtrar [0 para cancelar]: ");
                if (!termoBusca.equals("CANCELAR")) {
                    filtroProdutoAtivo = termoBusca;
                    paginaAtual = 1;
                }
            } else if (comandoUsuario.equals("L")) {
                filtroProdutoAtivo = "";
                paginaAtual = 1;
                System.out.println("✅ Filtro removido. Exibindo catálogo completo.\n");
            } else if (comandoUsuario.equals("0") || comandoUsuario.equals("-0") || comandoUsuario.equals("SAIR")) {
                navegandoLista = false;
            } else if (comandoUsuario.matches("^[0-9]+$")) {
                int posicaoNaListaFiltrada = Integer.parseInt(comandoUsuario) - 1;
                if (posicaoNaListaFiltrada >= 0 && posicaoNaListaFiltrada < totalFiltrados) {
                    int indiceSelecionado = listaIndicesReaisFiltrados.get(posicaoNaListaFiltrada);
                    boolean modalAberto = true;
                    while (modalAberto) {
                        if (indiceSelecionado < 0 || indiceSelecionado >= tabelaProdNome.size()) {
                            System.out.println("⚠️ Este produto já foi excluído.\n");
                            break;
                        }
                        System.out.println("\n┌────────────────────────────────────────────────────────┐");
                        System.out.println("│               [MODAL DE GESTÃO DE PRODUTO]             │");
                        System.out.println("├────────────────────────────────────────────────────────┤");
                        System.out.printf("│ Produto: %s\n", tabelaProdNome.get(indiceSelecionado));
                        System.out.printf("│ Preço: %s | Estoque: %d un\n", formatarValorMonetarioReais(tabelaProdPrecoVenda.get(indiceSelecionado)), tabelaProdQtdEstoque.get(indiceSelecionado));
                        System.out.println("├────────────────────────────────────────────────────────┤");
                        System.out.println("│ [1] Ver Ficha Fiscal e Técnica                         │");
                        System.out.println("│ [2] Editar Preço de Venda                              │");
                        System.out.println("│ [3] Ajustar Saldo em Estoque                           │");
                        System.out.println("│ [4] Excluir Produto (Exclusivo Gerente)                │");
                        System.out.println("│ [0] Fechar Modal                                       │");
                        System.out.println("└────────────────────────────────────────────────────────┘");

                        int acaoEscolhida = validarELerNumeroInteiro(leitorEntrada, "Ação no Modal: ");
                        if (acaoEscolhida == 1) {
                            System.out.println("\n--- FICHA DO PRODUTO ---");
                            System.out.println("• Nome      : " + tabelaProdNome.get(indiceSelecionado));
                            System.out.println("• NCM       : " + tabelaProdNcm.get(indiceSelecionado));
                            System.out.println("• Preço     : " + formatarValorMonetarioReais(tabelaProdPrecoVenda.get(indiceSelecionado)));
                            System.out.println("• Estoque   : " + tabelaProdQtdEstoque.get(indiceSelecionado) + " unidades\n");
                        } else if (acaoEscolhida == 2) {
                            if (!validarPermissaoGerencialOuFinanceira(cargoUsuarioLogado)) continue;
                            double novoPreco = validarELerValorDecimalMonetario(leitorEntrada, "Novo preço de venda (R$) [0 para cancelar]: ", false);
                            if (novoPreco > 0) {
                                tabelaProdPrecoVenda.set(indiceSelecionado, novoPreco);
                                registrarEventoAuditoriaSistema(tabelaLogAuditoria, nomeUsuarioLogado, "Alterou preço de " + tabelaProdNome.get(indiceSelecionado));
                                System.out.println("✅ Preço atualizado!\n");
                            }
                        } else if (acaoEscolhida == 3) {
                            if (!validarPermissaoGerencialOuFinanceira(cargoUsuarioLogado)) continue;
                            int novoEstoque = validarELerNumeroInteiro(leitorEntrada, "Nova quantidade em estoque [0 para cancelar]: ");
                            if (novoEstoque != -999 && novoEstoque >= 0) {
                                tabelaProdQtdEstoque.set(indiceSelecionado, novoEstoque);
                                registrarEventoAuditoriaSistema(tabelaLogAuditoria, nomeUsuarioLogado, "Ajustou estoque de " + tabelaProdNome.get(indiceSelecionado));
                                System.out.println("✅ Estoque atualizado!\n");
                            }
                        } else if (acaoEscolhida == 4) {
                            if (!validarPermissaoExclusivaGerente(cargoUsuarioLogado)) continue;
                            String nomeRemovido = tabelaProdNome.get(indiceSelecionado);
                            tabelaProdNome.remove(indiceSelecionado);
                            tabelaProdNcm.remove(indiceSelecionado);
                            tabelaProdPrecoVenda.remove(indiceSelecionado);
                            tabelaProdQtdEstoque.remove(indiceSelecionado);
                            registrarEventoAuditoriaSistema(tabelaLogAuditoria, nomeUsuarioLogado, "Excluiu produto: " + nomeRemovido);
                            System.out.println("🗑️ Produto '" + nomeRemovido + "' EXCLUÍDO do catálogo! Não está mais disponível para venda.\n");
                            modalAberto = false;
                        } else if (acaoEscolhida == 0 || acaoEscolhida == -999) {
                            modalAberto = false;
                        } else {
                            System.out.println("❌ Opção inválida.\n");
                        }
                    }
                } else {
                    System.out.println("❌ Produto inexistente ou já excluído.\n");
                }
            } else {
                System.out.println("❌ Comando inválido.\n");
            }
        }
    }

    private static void gerenciarHistoricoTransacoesPaginado(
            Scanner leitorEntrada,
            List<String> tabelaTransacaoTipo, List<String> tabelaTransacaoEnvolvidos,
            List<String> tabelaTransacaoDescricao, List<Double> tabelaTransacaoValor,
            List<Double> indicadorSaldoCaixaGlobal, List<String> tabelaLogAuditoria,
            String nomeUsuarioLogado, String cargoUsuarioLogado) {

        if (tabelaTransacaoTipo.isEmpty()) {
            System.out.println("⚠️ Nenhuma transação registrada.\n");
            return;
        }

        int paginaAtual = 1;
        final int ITENS_POR_PAGINA = 25;
        String filtroTransacaoAtivo = "";
        boolean navegandoLista = true;

        while (navegandoLista) {
            if (tabelaTransacaoTipo.isEmpty()) {
                System.out.println("ℹ️ Todas as transações foram estornadas/excluídas.\n");
                break;
            }

            List<Integer> listaIndicesReaisFiltrados = new ArrayList<>();
            for (int i = 0; i < tabelaTransacaoTipo.size(); i++) {
                String tipo = tabelaTransacaoTipo.get(i);
                String envolvidos = tabelaTransacaoEnvolvidos.get(i);
                String descricao = tabelaTransacaoDescricao.get(i);

                if (filtroTransacaoAtivo.isEmpty() ||
                    tipo.toUpperCase().contains(filtroTransacaoAtivo) ||
                    envolvidos.toUpperCase().contains(filtroTransacaoAtivo) ||
                    descricao.toUpperCase().contains(filtroTransacaoAtivo)) {
                    listaIndicesReaisFiltrados.add(i);
                }
            }

            int totalFiltrados = listaIndicesReaisFiltrados.size();
            int totalPaginas = Math.max(1, (int) Math.ceil((double) totalFiltrados / ITENS_POR_PAGINA));
            if (paginaAtual > totalPaginas) paginaAtual = totalPaginas;

            String etiquetaFiltro = filtroTransacaoAtivo.isEmpty() ? "TODAS AS OPERAÇÕES" : "FILTRO: '" + filtroTransacaoAtivo + "'";
            System.out.println("\n--- HISTÓRICO DE TRANSAÇÕES [" + etiquetaFiltro + "] (Página " + paginaAtual + " de " + totalPaginas + " | Encontrados: " + totalFiltrados + ") ---");

            if (totalFiltrados == 0) {
                System.out.println("⚠️ [ALERTA DE FILTRO]: Nenhuma transação corresponde ao filtro '" + filtroTransacaoAtivo + "'. Pressione [L] para limpar o filtro.");
            } else {
                int indiceInicial = (paginaAtual - 1) * ITENS_POR_PAGINA;
                int indiceFinal = Math.min(indiceInicial + ITENS_POR_PAGINA, totalFiltrados);

                for (int i = indiceInicial; i < indiceFinal; i++) {
                    int indiceReal = listaIndicesReaisFiltrados.get(i);
                    System.out.printf("[%d] %s | Valor: %s | %s\n",
                            (i + 1), tabelaTransacaoTipo.get(indiceReal),
                            formatarValorMonetarioReais(tabelaTransacaoValor.get(indiceReal)),
                            tabelaTransacaoDescricao.get(indiceReal));
                }
            }

            System.out.println("----------------------------------------------------------------");
            System.out.println("[N] Próxima | [P] Anterior | [F] Filtrar Tipo/Palavra-Chave | [L] Limpar Filtro | [NÚMERO] Abrir Modal | [0] Voltar");
            System.out.print("Comando: ");
            String comandoUsuario = leitorEntrada.nextLine().trim().toUpperCase();

            if (comandoUsuario.equals("N") && paginaAtual < totalPaginas) {
                paginaAtual++;
            } else if (comandoUsuario.equals("P") && paginaAtual > 1) {
                paginaAtual--;
            } else if (comandoUsuario.equals("F")) {
                String termoBusca = validarELerTextoLivreObrigatorio(leitorEntrada, "Digite o tipo (VENDA, PAGAMENTO, COMPRA) ou palavra-chave [0 para cancelar]: ");
                if (!termoBusca.equals("CANCELAR")) {
                    filtroTransacaoAtivo = termoBusca;
                    paginaAtual = 1;
                }
            } else if (comandoUsuario.equals("L")) {
                filtroTransacaoAtivo = "";
                paginaAtual = 1;
                System.out.println("✅ Filtro removido. Exibindo todas as transações.\n");
            } else if (comandoUsuario.equals("0") || comandoUsuario.equals("-0") || comandoUsuario.equals("SAIR")) {
                navegandoLista = false;
            } else if (comandoUsuario.matches("^[0-9]+$")) {
                int posicaoNaListaFiltrada = Integer.parseInt(comandoUsuario) - 1;
                if (posicaoNaListaFiltrada >= 0 && posicaoNaListaFiltrada < totalFiltrados) {
                    int indiceSelecionado = listaIndicesReaisFiltrados.get(posicaoNaListaFiltrada);
                    boolean modalAberto = true;
                    while (modalAberto) {
                        if (indiceSelecionado < 0 || indiceSelecionado >= tabelaTransacaoTipo.size()) {
                            System.out.println("⚠️ Esta transação já foi excluída.\n");
                            break;
                        }
                        System.out.println("\n┌────────────────────────────────────────────────────────┐");
                        System.out.println("│               [MODAL DE TRANSAÇÃO FINANCEIRA]          │");
                        System.out.println("├────────────────────────────────────────────────────────┤");
                        System.out.printf("│ Operação: %s | Montante: %s\n", tabelaTransacaoTipo.get(indiceSelecionado), formatarValorMonetarioReais(tabelaTransacaoValor.get(indiceSelecionado)));
                        System.out.printf("│ Envolvidos: %s\n", tabelaTransacaoEnvolvidos.get(indiceSelecionado));
                        System.out.println("├────────────────────────────────────────────────────────┤");
                        System.out.println("│ [1] Ver Comprovante Detalhado                          │");
                        System.out.println("│ [2] Editar Descrição do Lançamento                     │");
                        System.out.println("│ [3] Estornar e Excluir Lançamento (Exclusivo Gerente)  │");
                        System.out.println("│ [0] Fechar Modal                                       │");
                        System.out.println("└────────────────────────────────────────────────────────┘");

                        int acaoEscolhida = validarELerNumeroInteiro(leitorEntrada, "Ação no Modal: ");
                        if (acaoEscolhida == 1) {
                            System.out.println("\n--- COMPROVANTE DA OPERAÇÃO #" + (indiceSelecionado + 1) + " ---");
                            System.out.println("• Natureza   : " + tabelaTransacaoTipo.get(indiceSelecionado));
                            System.out.println("• Partes     : " + tabelaTransacaoEnvolvidos.get(indiceSelecionado));
                            System.out.println("• Histórico  : " + tabelaTransacaoDescricao.get(indiceSelecionado));
                            System.out.println("• Valor      : " + formatarValorMonetarioReais(tabelaTransacaoValor.get(indiceSelecionado)));
                            System.out.println("----------------------------------------------------------------\n");
                        } else if (acaoEscolhida == 2) {
                            if (!validarPermissaoGerencialOuFinanceira(cargoUsuarioLogado)) continue;
                            String novaDescricao = validarELerTextoLivreObrigatorio(leitorEntrada, "Nova descrição do lançamento [0 para cancelar]: ");
                            if (!novaDescricao.equals("CANCELAR")) {
                                tabelaTransacaoDescricao.set(indiceSelecionado, novaDescricao);
                                registrarEventoAuditoriaSistema(tabelaLogAuditoria, nomeUsuarioLogado, "Editou descrição da transação #" + (indiceSelecionado + 1));
                                System.out.println("✅ Lançamento atualizado!\n");
                            }
                        } else if (acaoEscolhida == 3) {
                            if (!validarPermissaoExclusivaGerente(cargoUsuarioLogado)) continue;
                            double valorEstorno = tabelaTransacaoValor.get(indiceSelecionado);
                            if (tabelaTransacaoTipo.get(indiceSelecionado).equalsIgnoreCase("VENDA")) {
                                indicadorSaldoCaixaGlobal.set(0, indicadorSaldoCaixaGlobal.get(0) - valorEstorno);
                            } else {
                                indicadorSaldoCaixaGlobal.set(0, indicadorSaldoCaixaGlobal.get(0) + valorEstorno);
                            }
                            tabelaTransacaoTipo.remove(indiceSelecionado);
                            tabelaTransacaoEnvolvidos.remove(indiceSelecionado);
                            tabelaTransacaoDescricao.remove(indiceSelecionado);
                            tabelaTransacaoValor.remove(indiceSelecionado);
                            registrarEventoAuditoriaSistema(tabelaLogAuditoria, nomeUsuarioLogado, "Estornou transação #" + (indiceSelecionado + 1));
                            System.out.println("🗑️ Transação ESTORNADA E EXCLUÍDA! Saldo em caixa recalculado.\n");
                            modalAberto = false;
                        } else if (acaoEscolhida == 0 || acaoEscolhida == -999) {
                            modalAberto = false;
                        } else {
                            System.out.println("❌ Opção inválida.\n");
                        }
                    }
                } else {
                    System.out.println("❌ Transação inexistente ou já excluída.\n");
                }
            } else {
                System.out.println("❌ Comando inválido.\n");
            }
        }
    }

    /* #########################################################################
       #                                                                       #
       #   MÓDULO 9: PORTAL DO CLIENTE E ORQUESTRAÇÃO PRINCIPAL (MAIN)         #
       #                                                                       #
       ######################################################################### */

    private static int obterQuantidadeReservadaNoCarrinho(int indiceProduto, List<Integer> carrinhoIndicesProdutos, List<Integer> carrinhoQuantidades) {
        int pos = carrinhoIndicesProdutos.indexOf(indiceProduto);
        return (pos != -1) ? carrinhoQuantidades.get(pos) : 0;
    }

    private static boolean gerenciarCarrinhoEFinalizarCompra(
            Scanner leitorEntrada,
            List<Integer> carrinhoIndicesProdutos, List<Integer> carrinhoQuantidades,
            List<String> tabelaProdNome, List<String> tabelaProdNcm, List<Double> tabelaProdPrecoVenda, List<Integer> tabelaProdQtdEstoque,
            List<Double> indicadorSaldoCaixaGlobal, List<Double> indicadorReceitaBrutaVendas,
            List<Double> indicadorCustoMercadoriaVendida, List<Double> indicadorImpostosProvisionados,
            List<String> tabelaClienteCpf, List<String> tabelaClienteNome,
            List<String> tabelaTransacaoTipo, List<String> tabelaTransacaoEnvolvidos,
            List<String> tabelaTransacaoDescricao, List<Double> tabelaTransacaoValor, int anoExercicioFiscal) {

        boolean gerenciandoCarrinho = true;
        while (gerenciandoCarrinho) {
            if (carrinhoIndicesProdutos.isEmpty()) {
                System.out.println("\n🛒 Seu carrinho está vazio.\n");
                return false;
            }

            double subtotalGeral = 0.0;
            double totalImpostosGeral = 0.0;
            int totalUnidades = 0;

            System.out.println("\n┌────────────────────────────────────────────────────────────────────────┐");
            System.out.println("│                       🛒 CARRINHO DE COMPRAS                           │");
            System.out.println("├────────────────────────────────────────────────────────────────────────┤");
            for (int i = 0; i < carrinhoIndicesProdutos.size(); i++) {
                int idxProd = carrinhoIndicesProdutos.get(i);
                int qtd = carrinhoQuantidades.get(i);
                double precoUnit = tabelaProdPrecoVenda.get(idxProd);
                double subtotalItem = precoUnit * qtd;
                double aliquota = calcularAliquotaIcmsPorOperacao(tabelaProdNcm.get(idxProd), "SP", "SP", anoExercicioFiscal);

                subtotalGeral += subtotalItem;
                totalImpostosGeral += (subtotalItem * aliquota);
                totalUnidades += qtd;

                System.out.printf("│ [%d] %s | %dx %s = %s\n",
                        (i + 1), tabelaProdNome.get(idxProd), qtd,
                        formatarValorMonetarioReais(precoUnit), formatarValorMonetarioReais(subtotalItem));
            }
            System.out.println("├────────────────────────────────────────────────────────────────────────┤");
            System.out.printf("│ Total de Itens: %d un | ICMS Embutido: %s | TOTAL A PAGAR: %s\n",
                    totalUnidades, formatarValorMonetarioReais(totalImpostosGeral), formatarValorMonetarioReais(subtotalGeral));
            System.out.println("└────────────────────────────────────────────────────────────────────────┘");
            System.out.println("[K] Finalizar e Pagar Pedido | [R] Remover Item | [E] Esvaziar Carrinho | [0] Continuar Comprando");
            System.out.print("Opção no Carrinho: ");
            String comandoCarrinho = leitorEntrada.nextLine().trim().toUpperCase();

            if (comandoCarrinho.equals("0") || comandoCarrinho.equals("-0") || comandoCarrinho.equals("SAIR")) {
                gerenciandoCarrinho = false;
            } else if (comandoCarrinho.equals("E")) {
                carrinhoIndicesProdutos.clear();
                carrinhoQuantidades.clear();
                System.out.println("🗑️ Carrinho esvaziado com sucesso!\n");
                return false;
            } else if (comandoCarrinho.equals("R")) {
                int itemRemover = validarELerNumeroInteiro(leitorEntrada, "Número do item no carrinho para remover [0 para cancelar]: ");
                if (itemRemover != -999) {
                    int idxCarrinho = itemRemover - 1;
                    if (idxCarrinho >= 0 && idxCarrinho < carrinhoIndicesProdutos.size()) {
                        String nomeRemovido = tabelaProdNome.get(carrinhoIndicesProdutos.get(idxCarrinho));
                        carrinhoIndicesProdutos.remove(idxCarrinho);
                        carrinhoQuantidades.remove(idxCarrinho);
                        System.out.println("🗑️ Item '" + nomeRemovido + "' removido do carrinho!\n");
                    } else {
                        System.out.println("❌ Item inválido no carrinho.\n");
                    }
                }
            } else if (comandoCarrinho.equals("K")) {
                String cpfComprador = validarEFormatarCpf(leitorEntrada);
                if (cpfComprador.equals("CANCELAR")) continue;

                String nomeComprador = "CLIENTE AVULSO";
                int indiceClienteCadastrado = tabelaClienteCpf.indexOf(cpfComprador);
                if (indiceClienteCadastrado != -1) {
                    nomeComprador = tabelaClienteNome.get(indiceClienteCadastrado);
                } else {
                    System.out.println("⚠️ CPF não encontrado na base de clientes cadastrados.");
                    System.out.print("Deseja cadastrar este novo cliente agora? [S/N]: ");
                    String desejaCadastrar = leitorEntrada.nextLine().trim().toUpperCase();
                    if (desejaCadastrar.equals("S")) {
                        String novoNomeCliente = validarELerTextoAlfabetico(leitorEntrada, "Nome completo do novo cliente: ");
                        if (!novoNomeCliente.equals("CANCELAR")) {
                            tabelaClienteCpf.add(cpfComprador);
                            tabelaClienteNome.add(novoNomeCliente);
                            nomeComprador = novoNomeCliente;
                            System.out.println("✅ Novo cliente cadastrado com sucesso e vinculado à venda!\n");
                        } else {
                            System.out.println("ℹ️ Cadastro cancelado. A venda prosseguirá como CLIENTE AVULSO.\n");
                        }
                    } else {
                        System.out.println("ℹ️ Venda prosseguirá como CLIENTE AVULSO.\n");
                    }
                }

                // Validação rigorosa de saldo em estoque antes da efetivação
                for (int i = 0; i < carrinhoIndicesProdutos.size(); i++) {
                    int idxProd = carrinhoIndicesProdutos.get(i);
                    int qtd = carrinhoQuantidades.get(i);
                    int estoqueAtualReal = tabelaProdQtdEstoque.get(idxProd);
                    if (tabelaProdNome.get(idxProd) == null || estoqueAtualReal < qtd) {
                        System.out.println("\n❌ [ALERTA DE ESTOQUE EXCEDIDO]: Falha no fechamento do pedido!");
                        System.out.printf("• Produto: %s\n• Solicitado: %d un | Estoque Atual Disponível: %d un\n",
                                tabelaProdNome.get(idxProd), qtd, Math.max(0, estoqueAtualReal));
                        System.out.println("⚠️ Ajuste as quantidades no carrinho para prosseguir com o pagamento.\n");
                        return false;
                    }
                }

                StringBuilder resumoItensPedido = new StringBuilder();
                for (int i = 0; i < carrinhoIndicesProdutos.size(); i++) {
                    int idxProd = carrinhoIndicesProdutos.get(i);
                    int qtd = carrinhoQuantidades.get(i);
                    tabelaProdQtdEstoque.set(idxProd, tabelaProdQtdEstoque.get(idxProd) - qtd);

                    if (i > 0) resumoItensPedido.append(", ");
                    resumoItensPedido.append(qtd).append("x ").append(tabelaProdNome.get(idxProd));
                }

                indicadorSaldoCaixaGlobal.set(0, indicadorSaldoCaixaGlobal.get(0) + subtotalGeral);
                indicadorReceitaBrutaVendas.set(0, indicadorReceitaBrutaVendas.get(0) + subtotalGeral);
                indicadorCustoMercadoriaVendida.set(0, indicadorCustoMercadoriaVendida.get(0) + (subtotalGeral * 0.45));
                indicadorImpostosProvisionados.set(0, indicadorImpostosProvisionados.get(0) + totalImpostosGeral);

                tabelaTransacaoTipo.add("VENDA");
                tabelaTransacaoEnvolvidos.add("Cliente: " + nomeComprador + " (CPF: " + cpfComprador + ")");
                tabelaTransacaoDescricao.add("Pedido Carrinho (" + carrinhoIndicesProdutos.size() + " itens): " + resumoItensPedido.toString());
                tabelaTransacaoValor.add(subtotalGeral);

                carrinhoIndicesProdutos.clear();
                carrinhoQuantidades.clear();

                System.out.println("\n================================================================");
                System.out.println("              ✅ PEDIDO CONCLUÍDO COM SUCESSO!                  ");
                System.out.println("================================================================");
                System.out.println("• Cliente       : " + nomeComprador + " (CPF: " + cpfComprador + ")");
                System.out.println("• Itens         : " + resumoItensPedido.toString());
                System.out.println("• Total Pago    : " + formatarValorMonetarioReais(subtotalGeral));
                System.out.println("• ICMS Embutido : " + formatarValorMonetarioReais(totalImpostosGeral));
                System.out.println("================================================================\n");
                return true;
            } else {
                System.out.println("❌ Opção inválida no carrinho.\n");
            }
        }
        return false;
    }

    public static void executarPortalComprasCliente(
            Scanner leitorEntrada,
            List<String> tabelaProdNome, List<String> tabelaProdNcm, List<Double> tabelaProdPrecoVenda, List<Integer> tabelaProdQtdEstoque,
            List<Double> indicadorSaldoCaixaGlobal, List<Double> indicadorReceitaBrutaVendas,
            List<Double> indicadorCustoMercadoriaVendida, List<Double> indicadorImpostosProvisionados,
            List<String> tabelaClienteCpf, List<String> tabelaClienteNome,
            List<String> tabelaTransacaoTipo, List<String> tabelaTransacaoEnvolvidos,
            List<String> tabelaTransacaoDescricao, List<Double> tabelaTransacaoValor, int anoExercicioFiscal) {

        if (tabelaProdNome.isEmpty()) {
            System.out.println("⚠️ Nenhum produto disponível na vitrine no momento.\n");
            return;
        }

        int paginaAtual = 1;
        final int ITENS_POR_PAGINA = 25;
        String filtroVitrineAtivo = "";
        boolean portalClienteAtivo = true;

        List<Integer> carrinhoIndicesProdutos = new ArrayList<>();
        List<Integer> carrinhoQuantidades = new ArrayList<>();

        while (portalClienteAtivo) {
            List<Integer> listaIndicesReaisFiltrados = new ArrayList<>();
            for (int i = 0; i < tabelaProdNome.size(); i++) {
                String nome = tabelaProdNome.get(i);
                if (nome != null) {
                    if (filtroVitrineAtivo.isEmpty() || nome.toUpperCase().contains(filtroVitrineAtivo)) {
                        listaIndicesReaisFiltrados.add(i);
                    }
                }
            }

            int totalFiltrados = listaIndicesReaisFiltrados.size();
            int totalPaginas = Math.max(1, (int) Math.ceil((double) totalFiltrados / ITENS_POR_PAGINA));
            if (paginaAtual > totalPaginas) paginaAtual = totalPaginas;

            int qtdTotalNoCarrinho = 0;
            double valorTotalNoCarrinho = 0.0;
            for (int i = 0; i < carrinhoIndicesProdutos.size(); i++) {
                int idx = carrinhoIndicesProdutos.get(i);
                int q = carrinhoQuantidades.get(i);
                qtdTotalNoCarrinho += q;
                valorTotalNoCarrinho += tabelaProdPrecoVenda.get(idx) * q;
            }

            String etiquetaFiltro = filtroVitrineAtivo.isEmpty() ? "TODOS OS PRODUTOS" : "FILTRO: '" + filtroVitrineAtivo + "'";
            System.out.println("\n--- VITRINE DE PRODUTOS [" + etiquetaFiltro + "] (Página " + paginaAtual + " de " + totalPaginas + ") ---");
            System.out.printf("🛒 CARRINHO ATUAL: %d un (%d tipos) | Subtotal: %s\n",
                    qtdTotalNoCarrinho, carrinhoIndicesProdutos.size(), formatarValorMonetarioReais(valorTotalNoCarrinho));
            System.out.println("----------------------------------------------------------------");

            if (totalFiltrados == 0) {
                System.out.println("⚠️ [ALERTA DE FILTRO]: Nenhum produto corresponde ao termo '" + filtroVitrineAtivo + "'. Pressione [L] para limpar o filtro ou refaça a busca.");
            } else {
                int indiceInicial = (paginaAtual - 1) * ITENS_POR_PAGINA;
                int indiceFinal = Math.min(indiceInicial + ITENS_POR_PAGINA, totalFiltrados);

                for (int i = indiceInicial; i < indiceFinal; i++) {
                    int indiceReal = listaIndicesReaisFiltrados.get(i);
                    int reservado = obterQuantidadeReservadaNoCarrinho(indiceReal, carrinhoIndicesProdutos, carrinhoQuantidades);
                    int disponivelParaAdicionar = tabelaProdQtdEstoque.get(indiceReal) - reservado;
                    String infoCarrinho = reservado > 0 ? " [No carrinho: " + reservado + "]" : "";

                    System.out.printf("[%d] %s | Preço: %s | Disponível: %d un%s\n",
                            (i + 1), tabelaProdNome.get(indiceReal),
                            formatarValorMonetarioReais(tabelaProdPrecoVenda.get(indiceReal)),
                            disponivelParaAdicionar, infoCarrinho);
                }
            }

            System.out.println("----------------------------------------------------------------");
            System.out.println("[N] Próxima | [P] Anterior | [F] Filtrar | [L] Limpar Filtro | [C] Ver Carrinho/Finalizar | [NÚMERO] Adicionar | [0] Voltar");
            System.out.print("Escolha: ");
            String comandoCliente = leitorEntrada.nextLine().trim().toUpperCase();

            if (comandoCliente.equals("N") && paginaAtual < totalPaginas) {
                paginaAtual++;
            } else if (comandoCliente.equals("P") && paginaAtual > 1) {
                paginaAtual--;
            } else if (comandoCliente.equals("F")) {
                String termoBusca = validarELerTextoLivreObrigatorio(leitorEntrada, "Digite o nome do produto para buscar [0 para cancelar]: ");
                if (!termoBusca.equals("CANCELAR")) {
                    filtroVitrineAtivo = termoBusca;
                    paginaAtual = 1;
                }
            } else if (comandoCliente.equals("L")) {
                filtroVitrineAtivo = "";
                paginaAtual = 1;
                System.out.println("✅ Filtro removido. Exibindo todos os produtos.\n");
            } else if (comandoCliente.equals("C")) {
                gerenciarCarrinhoEFinalizarCompra(
                        leitorEntrada, carrinhoIndicesProdutos, carrinhoQuantidades,
                        tabelaProdNome, tabelaProdNcm, tabelaProdPrecoVenda, tabelaProdQtdEstoque,
                        indicadorSaldoCaixaGlobal, indicadorReceitaBrutaVendas, indicadorCustoMercadoriaVendida, indicadorImpostosProvisionados,
                        tabelaClienteCpf, tabelaClienteNome, tabelaTransacaoTipo, tabelaTransacaoEnvolvidos, tabelaTransacaoDescricao, tabelaTransacaoValor,
                        anoExercicioFiscal
                );
            } else if (comandoCliente.equals("0") || comandoCliente.equals("-0") || comandoCliente.equals("SAIR")) {
                if (!carrinhoIndicesProdutos.isEmpty()) {
                    System.out.print("⚠️ Você possui itens não finalizados no carrinho. Deseja realmente sair e descartar? [S/N]: ");
                    String confirmaSaida = leitorEntrada.nextLine().trim().toUpperCase();
                    if (!confirmaSaida.equals("S")) continue;
                }
                portalClienteAtivo = false;
            } else if (comandoCliente.matches("^[0-9]+$")) {
                int posicaoNaListaFiltrada = Integer.parseInt(comandoCliente) - 1;
                if (posicaoNaListaFiltrada >= 0 && posicaoNaListaFiltrada < totalFiltrados) {
                    int indiceProduto = listaIndicesReaisFiltrados.get(posicaoNaListaFiltrada);
                    int reservado = obterQuantidadeReservadaNoCarrinho(indiceProduto, carrinhoIndicesProdutos, carrinhoQuantidades);
                    int saldoDisponivel = tabelaProdQtdEstoque.get(indiceProduto) - reservado;

                    if (saldoDisponivel <= 0) {
                        System.out.println("❌ [ALERTA DE ESTOQUE]: Sem saldo disponível! Todas as unidades já estão esgotadas ou reservadas no seu carrinho.\n");
                        continue;
                    }

                    int quantidadeDesejada = validarELerQuantidadePositiva(leitorEntrada, "Quantidade para adicionar ao carrinho (Máx disponível: " + saldoDisponivel + ") [0 para cancelar]: ");
                    if (quantidadeDesejada == -999) continue;
                    
                    if (quantidadeDesejada > saldoDisponivel) {
                        System.out.println("\n❌ [ALERTA DE ESTOQUE EXCEDIDO]: A quantidade solicitada (" + quantidadeDesejada + ") ultrapassa o saldo disponível (" + saldoDisponivel + " un).");
                        System.out.println("⚠️ Reduza a quantidade ou adicione até o limite máximo permitido.\n");
                        continue;
                    }

                    int posExistente = carrinhoIndicesProdutos.indexOf(indiceProduto);
                    if (posExistente != -1) {
                        carrinhoQuantidades.set(posExistente, carrinhoQuantidades.get(posExistente) + quantidadeDesejada);
                    } else {
                        carrinhoIndicesProdutos.add(indiceProduto);
                        carrinhoQuantidades.add(quantidadeDesejada);
                    }

                    System.out.println("✅ Adicionado ao carrinho: " + quantidadeDesejada + "x " + tabelaProdNome.get(indiceProduto) + "! Digite [C] quando quiser finalizar.\n");
                } else {
                    System.out.println("❌ Produto inválido ou removido do catálogo.\n");
                }
            } else {
                System.out.println("❌ Comando inválido.\n");
            }
        }
    }

    public static void exibirMenuPainelAdministrativo(
            Scanner leitorEntrada,
            String idUsuarioLogado, String nomeUsuarioLogado, String cargoUsuarioLogado, String senhaUsuarioLogado,
            List<String> tabelaFuncId, List<String> tabelaFuncNome, List<String> tabelaFuncCpf,
            List<String> tabelaFuncCargo, List<String> tabelaFuncSenha, List<Double> tabelaFuncSalario,
            List<Boolean> tabelaFuncPago, List<String> tabelaFuncStatus, List<String> tabelaFuncMotivoInativo,
            List<String> tabelaFornRazaoSocial, List<String> tabelaFornCnpj, List<String> tabelaFornUf,
            List<String> tabelaFornProdutoPrincipal, List<Double> tabelaFornValorFatura,
            List<Double> tabelaFornMargemLucroPct, List<Boolean> tabelaFornFaturaPaga,
            List<String> tabelaProdNome, List<String> tabelaProdNcm, List<Double> tabelaProdPrecoVenda, List<Integer> tabelaProdQtdEstoque,
            List<String> tabelaClienteCpf, List<String> tabelaClienteNome,
            List<Double> indicadorSaldoCaixaGlobal, List<Double> indicadorGastosEstoque, List<Double> indicadorReceitaBrutaVendas,
            List<Double> indicadorCustoMercadoriaVendida, List<Double> indicadorTotalSalariosPagos,
            List<Double> indicadorTotalFornecedoresPagos, List<Double> indicadorImpostosProvisionados,
            List<String> tabelaTransacaoTipo, List<String> tabelaTransacaoEnvolvidos,
            List<String> tabelaTransacaoDescricao, List<Double> tabelaTransacaoValor,
            List<String> tabelaLogAuditoria) {

        boolean sessaoAdminAtiva = true;
        while (sessaoAdminAtiva) {
            if (!verificarUsuarioLogadoAindaPossuiAutoridade(idUsuarioLogado, tabelaFuncId, tabelaFuncStatus)) return;

            System.out.println("\n================================================================");
            System.out.println("       PAINEL ADMINISTRATIVO PROTEI CREAT (" + cargoUsuarioLogado + ")");
            System.out.println("================================================================");
            if (cargoUsuarioLogado.equalsIgnoreCase("GERENTE")) {
                System.out.println("[1] Módulo RH: Autoridades e Funcionários");
                System.out.println("[2] Módulo Suprimentos: Fornecedores e Compras");
                System.out.println("[3] Módulo Financeiro, Estoque e Auditoria");
            } else if (cargoUsuarioLogado.equalsIgnoreCase("FINANCEIRO")) {
                System.out.println("[1] Módulo RH: Folha de Pagamento e Consulta");
                System.out.println("[2] Módulo Suprimentos: Fornecedores e Faturas");
                System.out.println("[3] Módulo Financeiro, Estoque e Histórico");
            } else {
                System.out.println("[3] Consultar Estoque e Histórico (Somente Leitura)");
            }
            System.out.println("[4] Encerrar Sessão Administrativa");
            System.out.println("================================================================");

            int opcaoPainel = validarELerNumeroInteiro(leitorEntrada, "Selecione o módulo: ");
            if (opcaoPainel == 1 && !cargoUsuarioLogado.equalsIgnoreCase("OPERADOR")) {
                exibirMenuGestaoFuncionarios(leitorEntrada, tabelaFuncId, tabelaFuncNome, tabelaFuncCpf, tabelaFuncCargo,
                        tabelaFuncSenha, tabelaFuncSalario, tabelaFuncPago, tabelaFuncStatus, tabelaFuncMotivoInativo,
                        tabelaLogAuditoria, idUsuarioLogado, nomeUsuarioLogado, cargoUsuarioLogado,
                        tabelaProdNome, tabelaProdNcm, tabelaProdPrecoVenda, tabelaProdQtdEstoque,
                        tabelaClienteCpf, tabelaClienteNome, indicadorSaldoCaixaGlobal, indicadorTotalSalariosPagos,
                        tabelaTransacaoTipo, tabelaTransacaoEnvolvidos, tabelaTransacaoDescricao, tabelaTransacaoValor);
            } else if (opcaoPainel == 2 && !cargoUsuarioLogado.equalsIgnoreCase("OPERADOR")) {
                exibirMenuGestaoFornecedores(leitorEntrada, tabelaFornRazaoSocial, tabelaFornCnpj, tabelaFornUf,
                        tabelaFornProdutoPrincipal, tabelaFornValorFatura, tabelaFornMargemLucroPct, tabelaFornFaturaPaga,
                        tabelaProdNome, tabelaProdNcm, tabelaProdPrecoVenda, tabelaProdQtdEstoque,
                        indicadorSaldoCaixaGlobal, indicadorGastosEstoque, indicadorTotalFornecedoresPagos,
                        tabelaTransacaoTipo, tabelaTransacaoEnvolvidos, tabelaTransacaoDescricao, tabelaTransacaoValor,
                        tabelaLogAuditoria, nomeUsuarioLogado, cargoUsuarioLogado);
            } else if (opcaoPainel == 3) {
                exibirMenuFinanceiroEstoqueEHistorico(leitorEntrada, tabelaProdNome, tabelaProdNcm, tabelaProdPrecoVenda, tabelaProdQtdEstoque,
                        tabelaClienteCpf, tabelaClienteNome, indicadorSaldoCaixaGlobal, indicadorGastosEstoque, indicadorReceitaBrutaVendas,
                        indicadorCustoMercadoriaVendida, indicadorTotalSalariosPagos,
                        indicadorTotalFornecedoresPagos, indicadorImpostosProvisionados,
                        tabelaTransacaoTipo, tabelaTransacaoEnvolvidos, tabelaTransacaoDescricao, tabelaTransacaoValor,
                        tabelaFuncId, tabelaFuncNome, tabelaFuncCpf, tabelaFuncCargo, tabelaFuncSenha,
                        tabelaFuncSalario, tabelaFuncPago, tabelaFuncStatus, tabelaFuncMotivoInativo,
                        tabelaLogAuditoria, idUsuarioLogado, nomeUsuarioLogado, cargoUsuarioLogado, senhaUsuarioLogado);
            } else if (opcaoPainel == 4 || opcaoPainel == -999) {
                sessaoAdminAtiva = false;
            } else {
                System.out.println("❌ Opção inválida para sua alçada.\n");
            }
        }
    }

    public static void main(String[] args) {
        Scanner leitorEntrada = new Scanner(System.in);

        System.out.println("\n================================================================");
        System.out.println("            BEM-VINDO AO PROTEI CREAT (L'ESTOQUE)               ");
        System.out.println("       SISTEMA MODULAR DE GESTÃO TRIBUTÁRIA E OPERACIONAL       ");
        System.out.println("================================================================");

        // --- TABELAS DE FUNCIONÁRIOS ---
        List<String> tabelaFuncId = new ArrayList<>();
        List<String> tabelaFuncNome = new ArrayList<>();
        List<String> tabelaFuncCpf = new ArrayList<>();
        List<String> tabelaFuncCargo = new ArrayList<>();
        List<String> tabelaFuncSenha = new ArrayList<>();
        List<Double> tabelaFuncSalario = new ArrayList<>();
        List<Boolean> tabelaFuncPago = new ArrayList<>();
        List<String> tabelaFuncStatus = new ArrayList<>();
        List<String> tabelaFuncMotivoInativo = new ArrayList<>();

        // --- TABELAS DE FORNECEDORES ---
        List<String> tabelaFornRazaoSocial = new ArrayList<>();
        List<String> tabelaFornCnpj = new ArrayList<>();
        List<String> tabelaFornUf = new ArrayList<>();
        List<String> tabelaFornProdutoPrincipal = new ArrayList<>();
        List<Double> tabelaFornValorFatura = new ArrayList<>();
        List<Double> tabelaFornMargemLucroPct = new ArrayList<>();
        List<Boolean> tabelaFornFaturaPaga = new ArrayList<>();

        // --- TABELAS DE CLIENTES ---
        List<String> tabelaClienteCpf = new ArrayList<>();
        List<String> tabelaClienteNome = new ArrayList<>();

        // --- TABELAS DE PRODUTOS E ESTOQUE ---
        List<String> tabelaProdNome = new ArrayList<>();
        List<String> tabelaProdNcm = new ArrayList<>();
        List<Double> tabelaProdPrecoVenda = new ArrayList<>();
        List<Integer> tabelaProdQtdEstoque = new ArrayList<>();

        // --- TABELAS DE HISTÓRICO FINANCEIRO E AUDITORIA ---
        List<String> tabelaTransacaoTipo = new ArrayList<>();
        List<String> tabelaTransacaoEnvolvidos = new ArrayList<>();
        List<String> tabelaTransacaoDescricao = new ArrayList<>();
        List<Double> tabelaTransacaoValor = new ArrayList<>();
        List<String> tabelaLogAuditoria = new ArrayList<>();

        // --- INDICADORES CONTÁBEIS CONSOLIDADOS ---
        List<Double> indicadorSaldoCaixaGlobal = new ArrayList<>(Arrays.asList(200000.00));
        List<Double> indicadorGastosEstoque = new ArrayList<>(Arrays.asList(35000.00));
        List<Double> indicadorReceitaBrutaVendas = new ArrayList<>(Arrays.asList(0.0));
        List<Double> indicadorCustoMercadoriaVendida = new ArrayList<>(Arrays.asList(0.0));
        List<Double> indicadorTotalSalariosPagos = new ArrayList<>(Arrays.asList(0.0));
        List<Double> indicadorTotalFornecedoresPagos = new ArrayList<>(Arrays.asList(0.0));
        List<Double> indicadorImpostosProvisionados = new ArrayList<>(Arrays.asList(0.0));
        int anoExercicioFiscal = 2026;

        inicializarTabelaFuncionarios(tabelaFuncId, tabelaFuncNome, tabelaFuncCpf, tabelaFuncCargo, tabelaFuncSenha, tabelaFuncSalario, tabelaFuncPago, tabelaFuncStatus, tabelaFuncMotivoInativo);
        inicializarTabelaFornecedores(tabelaFornRazaoSocial, tabelaFornCnpj, tabelaFornUf, tabelaFornProdutoPrincipal, tabelaFornValorFatura, tabelaFornMargemLucroPct, tabelaFornFaturaPaga);
        inicializarTabelaClientes(tabelaClienteCpf, tabelaClienteNome);

        System.out.println("\n[1] Inicializar Base no Modo Padrão Sincronizado");
        System.out.println("[2] Inicializar Base no Modo Randômico Dinâmico");
        int modoInicializacao = validarELerNumeroInteiro(leitorEntrada, "Escolha o modo de carga [1 ou 2]: ");

        popularBaseOperacional(
                (modoInicializacao == 2),
                tabelaProdNome, tabelaProdNcm, tabelaProdPrecoVenda, tabelaProdQtdEstoque,
                tabelaClienteNome,
                tabelaTransacaoTipo, tabelaTransacaoEnvolvidos, tabelaTransacaoDescricao, tabelaTransacaoValor,
                indicadorSaldoCaixaGlobal, indicadorReceitaBrutaVendas,
                indicadorCustoMercadoriaVendida, indicadorTotalFornecedoresPagos,
                indicadorTotalSalariosPagos, indicadorImpostosProvisionados,
                anoExercicioFiscal, tabelaLogAuditoria
        );

        boolean execucaoSistemaAtiva = true;
        while (execucaoSistemaAtiva) {
            System.out.println("\n================================================================");
            System.out.println("                 MENU PRINCIPAL DE ACESSO                       ");
            System.out.println("================================================================");
            System.out.println("[1] Portal do Cliente (Compras e Vitrine)");
            System.out.println("[2] Acesso Administrativo (Login de Colaborador)");
            System.out.println("[3] Encerrar Sistema");
            System.out.println("================================================================");

            int opcaoAcesso = validarELerNumeroInteiro(leitorEntrada, "Opção: ");
            if (opcaoAcesso == 1) {
                executarPortalComprasCliente(leitorEntrada, tabelaProdNome, tabelaProdNcm, tabelaProdPrecoVenda, tabelaProdQtdEstoque,
                        indicadorSaldoCaixaGlobal, indicadorReceitaBrutaVendas, indicadorCustoMercadoriaVendida, indicadorImpostosProvisionados,
                        tabelaClienteCpf, tabelaClienteNome, tabelaTransacaoTipo, tabelaTransacaoEnvolvidos, tabelaTransacaoDescricao, tabelaTransacaoValor,
                        anoExercicioFiscal);
            } else if (opcaoAcesso == 2) {
                System.out.println("\n=== AUTENTICAÇÃO DE COLABORADOR ===");
                for (int i = 0; i < tabelaFuncNome.size(); i++) {
                    if (tabelaFuncStatus.get(i).equalsIgnoreCase("ATIVO")) {
                        System.out.printf("- ID: %s | Nome: %s | Cargo: %s\n", tabelaFuncId.get(i), tabelaFuncNome.get(i), tabelaFuncCargo.get(i));
                    }
                }
                System.out.println("----------------------------------------------------------------");

                System.out.print("Informe seu ID Funcional (Ex: 001) [0 para voltar]: ");
                String idDigitado = leitorEntrada.nextLine().trim().toUpperCase();
                if (idDigitado.equals("0") || idDigitado.equals("-0") || idDigitado.equalsIgnoreCase("SAIR")) continue;

                int indiceUsuario = tabelaFuncId.indexOf(idDigitado);
                if (indiceUsuario == -1) {
                    System.out.println("❌ ID funcional inexistente.\n");
                    continue;
                }
                if (!tabelaFuncStatus.get(indiceUsuario).equalsIgnoreCase("ATIVO")) {
                    System.out.println("🚫 [ACESSO BLOQUEADO]: Este colaborador foi EXCLUÍDO/DEMITIDO e não possui mais autoridade no sistema!\n");
                    continue;
                }

                String nomeUsuarioLogado = tabelaFuncNome.get(indiceUsuario);
                String cargoUsuarioLogado = tabelaFuncCargo.get(indiceUsuario);
                String senhaCorreta = tabelaFuncSenha.get(indiceUsuario);
                boolean loginAutenticado = false;

                for (int tentativa = 1; tentativa <= 3; tentativa++) {
                    System.out.print("Senha (Tentativa " + tentativa + "/3): ");
                    String senhaDigitada = leitorEntrada.nextLine().trim();
                    if (senhaCorreta.equals(senhaDigitada)) {
                        loginAutenticado = true;
                        registrarEventoAuditoriaSistema(tabelaLogAuditoria, nomeUsuarioLogado, "Login realizado com sucesso");
                        System.out.println("\n✅ Bem-vindo(a), " + nomeUsuarioLogado + " [" + cargoUsuarioLogado + "]!\n");
                        break;
                    }
                    System.out.println("❌ Senha incorreta.\n");
                }

                if (loginAutenticado) {
                    exibirMenuPainelAdministrativo(leitorEntrada, idDigitado, nomeUsuarioLogado, cargoUsuarioLogado, senhaCorreta,
                            tabelaFuncId, tabelaFuncNome, tabelaFuncCpf, tabelaFuncCargo, tabelaFuncSenha,
                            tabelaFuncSalario, tabelaFuncPago, tabelaFuncStatus, tabelaFuncMotivoInativo,
                            tabelaFornRazaoSocial, tabelaFornCnpj, tabelaFornUf, tabelaFornProdutoPrincipal,
                            tabelaFornValorFatura, tabelaFornMargemLucroPct, tabelaFornFaturaPaga,
                            tabelaProdNome, tabelaProdNcm, tabelaProdPrecoVenda, tabelaProdQtdEstoque,
                            tabelaClienteCpf, tabelaClienteNome,
                            indicadorSaldoCaixaGlobal, indicadorGastosEstoque, indicadorReceitaBrutaVendas,
                            indicadorCustoMercadoriaVendida, indicadorTotalSalariosPagos,
                            indicadorTotalFornecedoresPagos, indicadorImpostosProvisionados,
                            tabelaTransacaoTipo, tabelaTransacaoEnvolvidos, tabelaTransacaoDescricao, tabelaTransacaoValor,
                            tabelaLogAuditoria);
                }
            } else if (opcaoAcesso == 3 || opcaoAcesso == -999) {
                execucaoSistemaAtiva = false;
                System.out.println("\n[SISTEMA] Protei Creat encerrado com segurança.\n");
            } else {
                System.out.println("❌ Opção inválida.\n");
            }
        }
        leitorEntrada.close();
    }
}
