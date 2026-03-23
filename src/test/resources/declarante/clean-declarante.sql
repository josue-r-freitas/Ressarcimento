-- Limpa a tabela declarante para testes que precisam de "nenhum declarante".
-- Funciona com H2 (perfil test). Ordem: arquivo_pedido e item_nota_saida podem referenciar nota_saida;
-- nota_saida referencia declarante. Para apenas GET declarante, basta limpar declarante se não houver FKs.
DELETE FROM item_nota_saida;
DELETE FROM nota_saida;
DELETE FROM arquivo_produtos;
DELETE FROM arquivo_pedido;
DELETE FROM nota_entrada;
DELETE FROM declarante;
