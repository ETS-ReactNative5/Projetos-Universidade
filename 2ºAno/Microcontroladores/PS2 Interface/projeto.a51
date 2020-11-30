#include <at89c51ic2.h>

K_LOAD		EQU P3.3	;BOTAO 1
K_SET		EQU P3.5	;BOTAO 2
PS2_DATA	EQU P2.0	;KEYBOARD DATA
PS2_CLOCK	EQU P3.2	;ClOCK PS2
DATA_BUFFER	EQU 50H		;DATA BUFFER
NEXT_STATE	EQU 60H		
START		EQU 00H
BYTE_S		EQU 01H
PARITY		EQU 02H
STOP		EQU 03H
FLAGS 		EQU	20H		
BITS		EQU R2		;Contador de bits a 1

DSEG	AT 20H
	BYTE_OK	BIT FLAGS.0			;Declaração de todas as flags necessária para avançar de estado
	BIT_START BIT FLAGS.1		;verificar se existem erros na trama(BYTE_OK)
	BIT_BYTE  BIT FLAGS.2		;BYTE_OK é a flag que verifica se existem erros na transmição
	BIT_PARITY  BIT FLAGS.3		;BIT_START,BIT_BYTE,BIT_PARITY,BIT_STOP são as flags correspondentes a cada estado
	BIT_STOP	 BIT FLAGS.4
	FLAG_BITS BIT FLAGS.5		
	PS2_OK BIT FLAGS.7			;PS2_OK é a flag correspondente á leitura da trama

CSEG	AT 000H
	JMP SETUP
CSEG 	AT 003H
	JMP EXT0_ISR
CSEG	AT 023H
	JMP RS232_ISR

CSEG	AT 33H
SETUP:
	SETB	K_LOAD
	SETB 	K_SET
	SETB	BIT_START			;Habilita-se a flag BIT_START para se começar a ler a trama quando houver a interrupção
	MOV 	NEXT_STATE, #START
	CALL 	RS232_CONFIG
	CALL 	EXT0_CONFIG
	SETB	EA
	
STATE_TABLE: 	DB 	026H,0C2H,015H,00CH		;START, BYTE, PARITY, STOP
	
MAIN:						;Mantém-se na MAIN até q ocorra a interrupção(interrupção externa) que habilida PSE2_OK
	JB 	PS2_OK, READ_PS2
	JMP MAIN

PRINT_STATE:
	MOV DPTR, #STATE_TABLE
	MOVC A, @A+DPTR 
	MOV P1, A
	RET
SELECT_STATE:
	MOV A, NEXT_STATE
	MOV DPTR, #JMP_TABLE
	RL A
	JMP @A+DPTR

STATE_START:			;estado onde se verifica o start bit		
	CLR BIT_START
	MOV A, #START
	CALL PRINT_STATE
	MOV R1, #8			;Contador dos 8 bits que se vão ler no estado a seguir
	MOV C, PS2_DATA		;Move o bit que esta em PS2_DATA(P2.0) para a flag carry
	JNC	BIT_START_OK	;Testa-se o start_bit e, caso esteja a 1 (existem erros na transmição) habilita-se a flag BYTE_OK
	SETB BYTE_OK
BIT_START_OK:
	SETB BIT_BYTE		;Habilita-se a flag BIT_BYTE para se poder ler os próximos 8 bits
	JMP MAIN

STATE_BYTE:				;estado onde se vai ler 8 bits(correspondentes á tecla) para a flag carry, um de cada vez.
	MOV A, #BYTE_S
	CALL PRINT_STATE
	MOV A, DATA_BUFFER
	MOV C, PS2_DATA
	JC COUNT_BITS 		;se a flag carry estiver a 1 incrementa-se BITS(linha 93) para ver a paridade
CONTINUE_BYTE:	
	RRC A
	MOV DATA_BUFFER, A
	CLR C
	DJNZ R1, NEXT		;Quando se lerem todos os 8 bits(R1 = 0), avança-se para o próximo estado através da flag BIT_PARITY
	CLR BIT_BYTE		;limpa-se a flag BIT_BYTE pois já se leram todos os 8 bits
	SETB BIT_PARITY
NEXT:
	JMP MAIN
	
COUNT_BITS:				;contador de BITS para a paridade
	INC BITS
	JB	FLAG_BITS, CONTINUE_PARITY		;condição quando se estiver a ler o bit de paridade
	JMP CONTINUE_BYTE

STATE_PARITY:			;estado onde se vê a paridade através do contador usado anteriormente
	CLR BIT_PARITY
	MOV A, #PARITY
	CALL PRINT_STATE
	MOV C, PS2_DATA		;Como este bit (10º bit da trama, o bit paridade) também está incluído na verificação de erros
	SETB FLAG_BITS		;Portanto entra no contador de BITS caso seja 1 
	JC COUNT_BITS
CONTINUE_PARITY:		;Calcula-se a paridade e se esta for impar ignora-se o Byte ou seja não se envia para o Host
	MOV A, BITS
	MOV B, #02H
	DIV AB
	MOV	A, B
	CJNE A, #00H, BIT_PARITY_NOT_OK
	SETB BIT_STOP
	JMP MAIN
BIT_PARITY_NOT_OK:
	SETB BYTE_OK
	SETB BIT_STOP
	JMP MAIN
	
STATE_STOP:				;estado onde se verifica o último bit da trama (stop bit)
	CLR BIT_STOP
	MOV A, #STOP			
	CALL PRINT_STATE
	MOV C, PS2_DATA
	JC BIT_STOP_OK		;Testa-se o stop bit e, caso o stop bit esteja a 0, habilita-se a flag BYTE_OK
	SETB BYTE_OK
BIT_STOP_OK:	
	JB	BYTE_OK, ERROR	;Caso se detete algum erro na transmição (BYTE_OK = 1),  não se envia para o Host a tecla/letra correspondente
	CALL TRANSLATE		
ERROR:					;Label onde se redefinem as vaiáveis para se poderem usar na próxima trama.
	CLR FLAG_BITS
	CLR BYTE_OK
	MOV R2, #00H
	SETB BIT_START
	JMP MAIN

READ_PS2:				;Label onde se lêm os 11 bits da trama(1 start bit, 8 bits de dados, 1 bit de paridade e 1 stop bit)
	CLR PS2_OK
	JB 	BIT_START, SEMI_START
	JB	BIT_BYTE,SEMI_BYTE
	JB	BIT_PARITY,SEMI_PARITY
	JB 	BIT_STOP,SEMI_STOP
	
//estados intermédios
SEMI_START:
	MOV NEXT_STATE, #START
	JMP SELECT_STATE
SEMI_BYTE:
	MOV NEXT_STATE, #BYTE_S
	JMP SELECT_STATE
SEMI_PARITY:
	MOV NEXT_STATE, #PARITY
	JMP SELECT_STATE	
SEMI_STOP:
	MOV NEXT_STATE, #STOP
	JMP SELECT_STATE
	
SEND_NUMBERS:			;Label onde se envia o número para o display de 7 segmentos
	SUBB A, #030H		;Como os números em ASCII estáo definidos entre 30 e 39, subtrai-se 30 a esse valor para se obter
	MOV DPTR, #NUMBERS	;o indice(de 0 a 9) que neste caso irá corresponder ao dígito através da tabela NUMBERS.
	MOVC A,@A+DPTR
	MOV P1,A
	RET
TRANSLATE:				;Label onde se converte o byte recebido do PS2 para ASCII e se for um número também se envia para o display de 7 segmentos
	MOV A, DATA_BUFFER
	MOV DPTR, #ASCII	
	MOVC A, @A+DPTR
	MOV R7, A
	CALL SEND_NUMBERS
	MOV A,R7
	MOV SBUF, A
	RET
	
//Tabela onde está traduzido para ASCII, por ordem crescente, o teclado PS2. Os * subsstituem todos os carateres não existentes no teclado
//e as teclas/carateres especiais.
ASCII: DB	"*********************Q1***ZSAW2**CXDE43***VFTR5**NBHGY6***MJU78***KIO09****L*P****************"
//Tabela dos números de 0 a 9 para se enviar para o display de 7 segmentos.	
NUMBERS: DB		0CH, 0CFH, 54H, 046H, 087H, 026H, 024H, 04FH, 004H, 006H	
	
RS232_ISR:
	;RX OU TX
	JB		RI, RS232_RX
	JB		TI,	RS232_TX

RS232_TX:
	MOV 	P1, #00CH
	CLR		TI
	RETI

RS232_RX:
	MOV 	P1, #0CFH
	MOV		A, SBUF
	CLR		RI
	RETI
	
EXT0_ISR:
	SETB PS2_OK
	RETI

RS232_CONFIG:
	MOV 	T2CON, #034H
	MOV		SCON, #050H
	MOV 	TH2, #0FFH
	MOV		TL2, #0D9H
	MOV		RCAP2H, #0FFH
	MOV		RCAP2L, #0D9H
	SETB	ES
	RET

EXT0_CONFIG:
	SETB P3.2
	SETB IT0
	SETB EX0
	RET
	
JMP_TABLE:
	AJMP STATE_START
	AJMP STATE_BYTE
	AJMP STATE_PARITY
	AJMP STATE_STOP
	
END