//	mp216TP1Gr06.c
//	TP1 - 2.4 (Cartão do Cidadão)

//	Leandro Alves		| A82157
//	J. Eduardo Santos	| A82350
//	David Machado		| A82381

//	Versão final

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#define MAX 100					//	Índice máximo das 'strings' e da estrutura

typedef struct citizen_card		//	Estrutura com os dados de cada cidadão
{
	int civil_id_no;			//	N.º ID Civil
	char name [MAX];			//	Nome
	char gender;				//	Sexo
	char adress [MAX];			//	Morada
	int tax_no;					//	N.º Identificação Fiscal
	int date_of_birth_day;		//	Data de Nascimento
	int date_of_birth_month;
	int date_of_birth_year;
}	info;

int entities = 0;				//	Índice de cada entidade (cidadão)
info citizen [MAX];				//	Declaração do vetor da estrutura

// Funções:
void Create ();
void Read ();
void Update ();
void Delete ();

int main ()
{	
	//	Criação e leitura do ficheiro:
	FILE *database;
	database = fopen ("citizen_database.txt", "r");
	if (database != NULL)
	{
		fread (&entities, sizeof (int), 1, database);	//	Leitura da entidade
		fread (citizen, sizeof (info), MAX, database);	//	Leitura da estrutura
		fclose (database);
	}
	int number = 0;

	printf ("\n(!) Welcome to the Citizen Database!\n");

	/*	Menu (enquanto o utilizador não pretender sair do programa, este escolherá
		o que pretende fazer durante a execução do programa):	*/
	while (number != 5)
	{
		printf ("\nWhat would you like to do?\n\n");
	
		printf ("1. Create a new entity;\n");
		printf ("2. Read an entities;\n");
		printf ("3. Update an entity;\n");
		printf ("4. Delete an entity;\n");
		printf ("5. Exit the program.\n");

		printf ("\n> Please, choose a number: ");
		scanf ("%d", &number);

		switch (number)	/*	O utilizador ao escolher um n.º de 1 a 4, chamará a
							respetiva função */
		{
			case 1: Create ();
			break;

			case 2: Read ();
			break;
			
			case 3: Update ();
			break;
		
			case 4: Delete ();
			break;
		
			default: if (number != 5)	/*	Se o utilizador escolher outro n.º,
											este será alertado pois escolheu um
											n.º errado */
			{
				printf ("(!) Invalid number!\n");
			}	break;
		}	//	Fim do 'switch'
	}		//	Fim do ciclo 'while'
	printf ("\n(!) Thank you for using the Citizen Database!\n\n");

	//	Armazenamento dos dados do programa no ficheiro:
	database = fopen ("citizen_database.txt", "w");

	fwrite (&entities, sizeof (int), 1, database);	//	Armazenamento da entidade
	fwrite (citizen, sizeof (info), MAX, database);	//	Armazenamento da estrutura
	fclose (database);

	return 0;
}
void Create ()	//	Função onde o utilizador criará cada entidade:
{
	int temp_civil_id_no = 0;	/*	Civil ID No. temporário que o utilizador irá
									escrever					*/
	int temp_entity = 0;		/*	Índice que servirá para procurar entidades já 
									existentes					*/
	int check_civil_id_no = 1;	/*	Variável que irá verificar se existem variáveis
									com o mesmo Civil ID No.	*/

	printf ("\nCreating an entity:\n\n");

	printf ("> Civil ID No.: ");
	scanf ("%d", &temp_civil_id_no);	//	Leitura da entidade temporária

	/*	O ciclo 'for' irá percorrer cada posição do vetor da estrutura até chegar
		ao n.º atual de entidades	*/
	for (temp_entity = 0; temp_entity <= entities; temp_entity ++)
	{
		/*	Se um 'Civil ID No.' igual ao ID temporário, a varável
			'check_civil_id_no' tomará o valor '0'	*/
		if (citizen[temp_entity].civil_id_no == temp_civil_id_no)
		{
			printf ("(!) That Civil ID No. is already being used!\n");
			check_civil_id_no = 0;
		}	//	Fim da condição 'if'
	}		//	Fim do ciclo 'for'
	/*	Se 'check_civil_id_no' tiver o valor '1', o utilizador preencherá cada dado
		do cidadão (membro da estrutura)	*/
	if (check_civil_id_no == 1)
	{
		citizen[entities].civil_id_no = temp_civil_id_no;

		printf ("> Name: ");
		scanf (" %[^\n]s", citizen[entities].name);

		printf ("> Gender (m/f): ");
		scanf (" %c", &citizen[entities].gender);
				
		printf ("> Adress: ");
		scanf (" %[^\n]s", citizen[entities].adress);

		printf ("> Tax No.: ");
		scanf ("%d", &citizen[entities].tax_no);

		printf ("> Date of Birth (dd mm yyyy): ");
		scanf ("%d %d %d", &citizen[entities].date_of_birth_day, &citizen[entities].date_of_birth_month, &citizen[entities].date_of_birth_year);

		entities ++;
	}	//	Fim da condição 'if'
}		//	Fim da função
void Read ()	//	Função onde o utilizador irá ler os dados de uma entidade:
{
	int temp_civil_id_no = 0;	/*	Civil ID No. temporário que o utilizador irá
									escrever					*/
	int temp_entity = 0;		/*	Índice que servirá para procurar entidades já 
									existentes					*/
	int check_civil_id_no = 0;	/*	Variável que irá verificar se existem variáveis
									com o Civil ID No. lido		*/

	//	Se não existirem entidades, o utilizador não poderá ler nada
	if (entities == 0)
	{
		printf ("(!) There are no entities to read, please create one!\n");
	}
	//	Se existirem, o utilizador fará a leitura dum ID temporário
	else
	{
		printf ("\nReading an entity:\n\n");

		printf ("> Civil ID No.: ");
		scanf ("%d", &temp_civil_id_no);

		/*	O ciclo 'for' irá percorrer cada posição do vetor da estrutura até
			chegar ao n.º atual de entidades	*/
		for (temp_entity = 0; temp_entity < entities; temp_entity ++)
		{
			/*	Se existir um ID igual ao temporário, os dados dessa entidade serão
				escritos no ecrã	*/
			if (citizen[temp_entity].civil_id_no == temp_civil_id_no)
			{
				printf ("> Name: %s\n", citizen[temp_entity].name);
				printf ("> Gender: ");
				switch (citizen[temp_entity].gender)
				{
					case 'm': printf ("Male\n");
					break;

					case 'M': printf ("Male\n");
					break;

					case 'f': printf ("Female\n");
					break;

					case 'F': printf ("Female\n");
					break;
				
					default: printf ("Apache Helicopter\n");
					break;
				}
				printf ("> Adress: %s\n", citizen[temp_entity].adress);
				printf ("> Tax No.: %d\n", citizen[temp_entity].tax_no);
				printf ("> Date of Birth: %d-%d-%d\n", citizen[temp_entity].date_of_birth_day, citizen[temp_entity].date_of_birth_month, citizen[temp_entity].date_of_birth_year);

				check_civil_id_no = 1;	/*	Como esta entidade existe, esta
											tomará o valor de '1'	*/
			}	//	Fim da condição 'if'
		}		//	Fim do ciclo 'for'
		//	Se não existir, o utilizador não poderá ler a entidade
		if (check_civil_id_no == 0)
		{
			printf ("(!) Entity not found!\n");
		}
	}			//	Fim da condição 'else'
}				//	Fim da função
void Update ()
{
	int temp_civil_id_no = 0;
	int temp_entity = 0;
	int check_civil_id_no = 0;

	if (entities == 0)
	{
		printf ("(!) There are no entities to update, please create one!\n");
	}
	else
	{
		printf ("\nUpdating an entity:\n\n");

		printf ("> Civil ID No.: ");
		scanf ("%d", &temp_civil_id_no);

		for (temp_entity = 0; temp_entity < entities; temp_entity ++)
		{
			if (citizen[temp_entity].civil_id_no == temp_civil_id_no)
			{
				printf ("> Name (%s): ", citizen[temp_entity].name);
				scanf (" %[^\n]s", citizen[temp_entity].name);

				printf ("> Gender (");
				switch (citizen[temp_entity].gender)
				{
					case 'm': printf ("Male): ");
					break;

					case 'M': printf ("Male): ");
					break;

					case 'f': printf ("Female): ");
					break;

					case 'F': printf ("Female): ");
					break;
				
					default: printf ("Apache Helicopter): ");
					break;
				}
				scanf (" %c", &citizen[temp_entity].gender);

				printf ("> Adress (%s): ", citizen[temp_entity].adress);
				scanf (" %[^\n]s", citizen[temp_entity].adress);

				printf ("> Tax No. (%d): ", citizen[temp_entity].tax_no);
				scanf ("%d", &citizen[temp_entity].tax_no);

				printf ("> Date of Birth (dd mm yyyy) (%d-%d-%d): ", citizen[temp_entity].date_of_birth_day, citizen[temp_entity].date_of_birth_month, citizen[temp_entity].date_of_birth_year);
				scanf ("%d %d %d", &citizen[temp_entity].date_of_birth_day, &citizen[temp_entity].date_of_birth_month, &citizen[temp_entity].date_of_birth_year);

				check_civil_id_no = 1;
			}
		}
		if (check_civil_id_no == 0)
		{
			printf ("(!) Entity not found!\n");
		}
	}
}
void Delete ()
{
	int temp_civil_id_no = 0;
	int temp_entity = 0;
	int temp2_entity = 0;
	int check_civil_id_no = 0;

	if (entities == 0)
	{
		printf ("(!) There are no entities to delete, please create one!\n");
	}
	else
	{
		printf ("\nDeleting an entity:\n\n");

		printf ("> Civil ID No.: ");
		scanf ("%d", &temp_civil_id_no);

		if (temp_entity < entities)
		{
			if (citizen[temp_entity].civil_id_no == temp_civil_id_no)
			{
				for (temp2_entity = entities; temp2_entity >= temp_entity; temp2_entity --)
				{
					citizen [temp2_entity] = citizen [temp2_entity - 1];
				}
				check_civil_id_no = 1;
			}
			if (check_civil_id_no == 1)
			{
				printf ("(!) The entity with the Civil ID No. %d has been deleted.\n", temp_civil_id_no);
				entities --;
			}
		}
		if (check_civil_id_no == 0)
		{
			printf ("(!) Entity not found!\n");
		}
	}
}