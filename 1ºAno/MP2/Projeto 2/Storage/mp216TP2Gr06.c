//	mp216TP2Gr06.c
//	TP2 | Armazém

//	Grupo 6 - PL1
//	Leandro Alves		| A82157
//	J. Eduardo Santos	| A82350
//	David Machado		| A82381

//	Versão final

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#define MAX 100				//	Índice máximo das 'strings'

typedef struct storage		//	Estrutura com os dados de cada jogo
{
	int id;					//	ID do jogo
	char name [MAX];		//	Nome
	float price;			//	Preço
	int stock;				//	Quantidade
	struct storage *next;	//	Estrutura recursiva
}	info, *pointer;			//	Apontador da estrutura

//	Funções
pointer Upload ();
void Update ();
pointer Delete ();
pointer Search ();
void Catalog ();
void Buy ();

//	Função que substitui o 'scanf' por 'fgets'
float ScanfSub (char temp [MAX], float temp_temp)
{
	temp [strlen (temp) - 1] = '\0';	//	Remove a linha ('\n') da variável
    temp_temp = atof (temp);			//	Converte para 'float' o valor de 'temp' e copia-o para uma nova variável

	return temp_temp;
}
//	Função que armazena ordenadamente os dados do armazém
pointer Insert (int temp_id, char temp_name [], float temp_price, int temp_stock, pointer ptr)
{
	pointer new;	//	Armazenamento dum novo jogo
	new = (pointer) malloc (sizeof (info));

	if (ptr)		//	Se o fim do armazém não for encontrado
	{
		if (temp_id < ptr -> id)	//	Se o ID for menor que o ID anteriormente armazenado, aquele ficará por cima deste
		{ 
			new -> id = temp_id;
			strcpy (new -> name, temp_name);
			new -> price = temp_price;
			new -> stock = temp_stock;
			new -> next = ptr;
			ptr = new;
		}
		else
		{
			//	Chamada recursiva para verificar o próximo jogo
			ptr -> next = Insert (temp_id, temp_name, temp_price, temp_stock, ptr -> next);
		}
	}
	else	//	Se o ID for maior do que todos os IDs, este ficará no fundo da lista
	{
		new -> id = temp_id;
		strcpy (new -> name, temp_name);
		new -> price = temp_price;
		new -> stock = temp_stock;
		new -> next = ptr;
		ptr = new;
	}
	return ptr;
}
//	Função principal
int main ()
{
	char temp [MAX];
	int temp_id;
	char temp_name [MAX];
	float temp_price;
	int temp_stock;
	int number = 0;

	pointer ptr = NULL;

	FILE *storage;									//	Apontador do ficheiro

	storage = fopen ("steam_storage.txt", "r");		//	Leitura do ficheiro

    if (storage)									//	Se o armazém não estiver vazio
	{
    	fgets (temp, MAX, storage);
    	temp_id = atoi (temp);

		while (!feof (storage))						//	Enquanto o armazenamento do ficheiro não chegar ao fim
		{
			fgets (temp_name, MAX, storage);
			temp_name [strlen (temp_name) - 1] = '\0';
			fgets (temp, MAX, storage);
    		temp_price = atof (temp);
    		fgets (temp, MAX, storage);
    		temp_stock = atoi (temp);

			//	Os dados da função são inseridos na estrutura
			ptr = Insert (temp_id, temp_name, temp_price, temp_stock, ptr);

			fgets (temp, MAX, storage);
    		temp_id = atoi (temp);
		}
		fclose (storage);							//	Fim da leitura do ficheiro
    }
    system ("clear");								//	Limpeza do terminal
    printf ("\n(!) Hi and welcome to the Steam Powered Storage, after 1 month in development, it was worth the wait.\n");
    printf ("(!) Thanks and have fun!\n");

	while (number != 7)								//	Enquanto o utilizador não quiser sair do programa
	{
		printf ("\n[STEAM POWERED STORAGE]\n\n");

		printf ("[Storage management]\n");
		printf ("1. Upload a game;\n");
		printf ("2. Update a game's data;\n");
		printf ("3. Delete a game;\n\n");

		printf ("[Clients area]\n");
		printf ("4. Search for a game;\n");
		printf ("5. Catalog;\n");
		printf ("6. Buy / sell a game;\n\n");

		printf ("7. Exit.\n\n");

		printf ("> Please, choose a number: ");
		fgets (temp, sizeof (temp), stdin);
		number = ScanfSub (temp, number);
		system ("clear");

		//	O utilizador ao escolher um número de 1 a 6, chamará a respetiva função
		switch (number)
		{
			case 1: printf ("\n[Uploading a game]\n\n");
                    ptr = Upload (ptr);
			break;

			case 2: if (ptr)	//	Se o armazém não estiver vazio
					{
						printf ("\n[Updating a game's data]\n\n");
                    	Update (ptr, temp_id);
                    }
                    else
					{
						printf ("\n(!) The storage is empty! Please, upload a game if you want to update its data.\n");
					}	break;

			case 3:	if (ptr)
					{
						printf ("\n[Deleting a game]\n\n");
						printf ("> ID: ");
	    	        	fgets (temp, sizeof (temp), stdin);
		            	temp_id = ScanfSub (temp, temp_id);
					 	ptr = Delete (ptr, temp_id);
					}
					else
					{
						printf ("\n(!) The storage is empty! Please, upload a game if you want to delete it.\n");
					}	break;
		
			case 4: if (ptr)
					{
						printf ("\n[Searching for a game]\n\n");
						Search (ptr);
					}
                    else
					{
						printf ("\n(!) The storage is empty! Please, upload a game if you want to search for it.\n");
					}	break;

			case 5: if (ptr)
					{
						printf ("\n[Catalog]\n\n");
						Catalog (ptr);
					}
					else
					{
						printf ("\n(!) The storage is empty! Please, upload a game if you want to see the catalog.\n");
					}	break;

			case 6: if (ptr)
					{
						printf ("\n[Buying / selling a game]\n\n");
						Buy (ptr);
					}
					else
					{
						printf ("\n(!) The storage is empty! Please, upload a game if you want to buy or sell it.\n");
					}	break;

			default: if (number != 7)
					 {
						printf ("\n[STEAM POWERED STORAGE]\n\n");
						printf ("> Please, choose a number: %d\n\n", number);
						printf ("(!) Invalid number!\n");
					 }	break;
		}
		if (number != 7)
		{
			printf ("\n(!) Press 'Enter' to continue.\n");
        	getchar ();
        	system ("clear");
        }
	}
	printf ("\n[STEAM POWERED STORAGE]\n\n");
	printf ("(!) Thank you for using the Steam Powered Storage!\n\n");

	storage = fopen ("steam_storage.txt", "w");	//	Armazenamento dos dados no ficheiro
	while (ptr)									//	Enquanto o fim do armazém não for encontrado
	{
		fprintf (storage, "%d\n%s\n%f\n%d\n", ptr -> id, ptr -> name, ptr -> price, ptr -> stock);
		ptr = ptr -> next;						//	Armazenamento do próximo jogo
	}
	fclose (storage);							//	Fim do armazenamento dos dados no ficheiro

	return 0;	//	Fim do programa
}
//	Função para carregar jogos no armazém
pointer Upload (pointer ptr)
{
	char temp [MAX];
	int temp_id;
	char temp_name [MAX];
	float temp_price;
	int temp_stock;
	int check_id = 1;
	pointer temp_ptr;
	temp_ptr = ptr;

	printf ("> ID: ");
	fgets (temp, sizeof (temp), stdin);
	temp_id = ScanfSub (temp, temp_id);

	while (temp_ptr)					//	Enquanto o fim do armazém não for encontrado
	{	
		if (temp_ptr -> id == temp_id)	//	Se já existir um ID igual ao introduzido
		{
			check_id = 0;
			printf ("\n(!) This ID is already being used by %s! Please, choose another one.\n", temp_ptr -> name);
		}
		temp_ptr = temp_ptr -> next;	//	Verificação do jogo seguinte
	}
	if (check_id == 1)
	{
		printf ("> Name: ");
		fgets (temp_name, sizeof (temp_name), stdin);
		temp_name [strlen (temp_name) - 1] = '\0';

		printf ("> Price: $");
		fgets (temp, sizeof (temp), stdin);
		temp_price = ScanfSub (temp, temp_price);
		if (temp_price < 0)	//	Se o preço for negativo, o programa assumirá como 0
		{
			temp_price = 0;
		}
		printf ("> Stock: ");
		fgets (temp, sizeof (temp), stdin);		
		temp_stock = ScanfSub (temp, temp_stock);
		if (temp_stock < 0)	//	Se o stock for negativo, o programa assumirá como 0
		{
			temp_stock = 0;
		}
		ptr = Insert (temp_id, temp_name, temp_price, temp_stock, ptr);	//	Armazenamento dos dados

		system ("clear");
		printf ("\n[Uploading a game]\n\n");
		printf ("(!) %s (%d) was successfully uploaded to the Storage!\n", temp_name, temp_id);
	}
	return ptr;
}
//	Função para atualizar os dados dum jogo
void Update (pointer ptr)
{	
	char temp [MAX];
	int temp_id;
	char temp_name [MAX];
	float temp_price;
	int temp_stock;
	int check_id = 0;

	printf ("> ID: ");
	fgets (temp, sizeof (temp), stdin);
	temp_id = ScanfSub (temp, temp_id);

	while (ptr)
	{
		if (temp_id == ptr -> id)
		{
			check_id = 1;

			printf ("> Name (%s): ", ptr -> name);
			fgets (temp_name, sizeof (temp), stdin);
			temp_name [strlen (temp_name) - 1] = '\0';
			strcpy (ptr -> name, temp_name);

			printf ("> Price ($%.2f): $", ptr -> price);
			fgets (temp, sizeof (temp), stdin);
			temp_price = ScanfSub (temp, temp_price);
			ptr -> price = temp_price;
			if (temp_price < 0)
			{
				temp_price = 0;
			}
			printf ("> Stock: (%d): ", ptr -> stock);
			fgets (temp, sizeof (temp), stdin);		
			temp_stock = ScanfSub (temp, temp_stock);
			if (temp_stock < 0)
			{
				temp_stock = 0;
			}
			ptr -> stock = temp_stock;

			system ("clear");
			printf ("\n[Updating a game's data]\n\n");
			printf ("(!) %s (%d) was successfully updated!\n", ptr -> name, ptr -> id);
		}
		ptr = ptr -> next;	//	Verificação do jogo seguinte
	}
	if (check_id == 0)		//	Se não existir um ID igual ao introduzido
	{
		printf ("\n(!) ID not found!\n");
	}
}
//	Função para apagar um jogo
pointer Delete (pointer ptr, int temp_id)
{
	char temp [MAX];
	int check_id = 0;

    if (ptr)	//	Se o fim do armazém não tiver sido encontrado
    {
    	check_id = 1;

		if (ptr -> id == temp_id)
   		{	
   			system ("clear");
   			printf ("\n[Deleting a game]\n\n");
			printf ("(!) %s (%d) has been deleted!\n", ptr -> name, ptr -> id);
			ptr = ptr -> next;
   		}
		else
   		{
			ptr -> next = Delete (ptr -> next, temp_id);	//	Chamada recursiva para testar o jogo seguinte
		}
   	}
	if (check_id == 0)
	{
		printf ("\n(!) ID not found!\n");
	}
	return ptr;
}
//	Função para ler os dados dum jogo
pointer Search (pointer ptr)
{
	char temp [MAX];
	int temp_id;
	int check_id = 0;

	printf ("> ID: ");
	fgets (temp, sizeof (temp), stdin);
	temp_id = ScanfSub (temp, temp_id);

	while (ptr)
	{
		if (ptr -> id == temp_id)
		{
			check_id = 1;

			printf ("> Name: %s\n", ptr -> name);
			if (ptr -> price != 0)
			{
				printf ("> Price: $%.2f\n", ptr -> price);
			}
			else
			{
				printf ("> Free to Play\n");
			}
			printf ("> Stock: %d\n", ptr -> stock);
		}
		ptr = ptr -> next;
	}
	if (check_id == 0)
	{
		printf ("\n(!) ID not found!\n");
	}
}
//	Função para ler o catálogo completo dos jogos
void Catalog (pointer ptr)
{
	if (ptr)
	{
		if (ptr -> price != 0)
		{
			printf ("* ID: %d | Name: %s | Price: $%.2f | Stock: %d\n", ptr -> id, ptr -> name, ptr -> price, ptr -> stock);
		}
		else
		{
			printf ("* ID: %d | Name: %s | Free to Play | Stock: %d\n", ptr -> id, ptr -> name, ptr -> stock);
		}
		Catalog (ptr -> next);	//	Chamada recursiva para ler o jogo seguinte
	}
}
//	Função para comprar ou vender um jogo
void Buy (pointer ptr)
{
	char temp [MAX];
	int temp_id;
	float temp_price;
	int copies;
	int temp_stock;
	int check_id = 0;

	printf ("> ID: ");
	fgets (temp, sizeof (temp), stdin);
	temp_id = ScanfSub (temp, temp_id);

	while (ptr)
	{
		if (temp_id == ptr -> id)
		{
			check_id = 1;
			
			printf ("> Name: %s\n", ptr -> name);

			if (ptr -> price != 0)
			{
				printf ("> Price: $%.2f\n", ptr -> price);
			}
			else
			{
				printf ("> Price: Free to Play\n");
			}
			if (ptr -> stock > 0)
			{
				printf ("> Copies (%d in stock) (> 0 to buy, < 0 to sell, = 0 to cancel): ", ptr -> stock);
			}
			else
			{
				printf ("> Copies (empty stock) (< 0 to sell, = 0 to cancel): ");
			}
			fgets (temp, sizeof (temp), stdin);
			copies = ScanfSub (temp, copies);
			system ("clear");
					
			if (copies <= ptr -> stock && copies != 0)	//	Se as cópias pedidas forem menores do que o stock e forem diferentes de 0
			{
				printf ("\n[Checkout]\n\n");

				printf ("> ID: %d\n", ptr -> id);
				printf ("> Name: %s\n", ptr -> name);
				
				if (copies > 0)
				{
					printf ("> Copies to buy: %d\n", copies);
					printf ("> Total amount to pay: $%.2f\n", ptr -> price * copies);
				}
				else
				{
					printf ("> Copies to sell: %d\n", -copies);
					printf ("> Total amount to receive: $%.2f\n", ptr -> price * (-copies));
				}
				printf ("\n(!) Press 'Enter' to confirm.");
				getchar ();
				system ("clear");
				printf ("\n[Checkout]\n\n");

				if (copies == 1)
				{
					printf ("(!) 1 copy of %s was added to your inventory, enjoy it and have fun!\n", ptr -> name);
				}
				else if (copies == -1)
				{
					printf ("(!) 1 copy of %s was sold and taken out from your inventory!\n", ptr -> name);
				}
				else if (copies > 1)
				{
					printf ("(!) %d copies of %s were added to your inventory, enjoy them and have fun!\n", copies, ptr -> name);
				}
				else
				{
					printf ("(!) %d copies of %s were sold and taken out from your inventory!\n", -copies, ptr -> name);
				}
				temp_stock = ptr -> stock;
				temp_stock = temp_stock - copies;
				ptr -> stock = temp_stock;
			}
			else if (copies != 0)
			{
				printf ("\n[Buying / selling a game]\n\n");
				printf ("> Copies: %d\n\n", copies);
				if (ptr -> stock <= 0)
				{
					printf ("(!) The stock for %s is empty, likewise there are no copies to be sold!\n", ptr -> name);
				}
				else if (ptr -> stock == 1)
				{
					printf ("(!) There is only 1 copy of %s in stock!\n", ptr -> name);
				}
				else
				{
					printf ("(!) There are only %d copies of %s in stock!\n", ptr -> stock, ptr -> name);
				}
				printf ("(!) If you need more copies, please update the game's data and refill the stock. ");
				printf ("Otherwise, please choose a lower number of copies.\n");
			}
			else
			{
				printf ("\n[Buying / selling a game]\n\n");
				printf ("(!) Purchase cancelled!\n");
			}
		}
		ptr = ptr -> next;
	}
	if (check_id == 0)
	{
		printf ("\n(!) ID not found!\n");
	}
}