/* PROJECT MIETI 2016/2017 Métodos de Programação I*/
/* Made by: Leandro Alves nº A82157    */
/* Last edit: 14/01/2017     */
/* FINAL VERSION   */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

int convert(char *word, char *aux_w){ 
    //Scroll through all the letters of the word and convert the whole word to morse
    int i;strcpy(aux_w,"");              
    // "Clean" the variable before using it
    for (i=0;i<strlen(word);i++){              
        //Enter each letter of the word to be converted and as we move from letter
        switch(word[i]){ 
            //to letter until we reach the end of the word we are converting and joining them
            case 'A': strcat(aux_w,".-"); break;
            case 'B': strcat(aux_w,"-..."); break;
            case 'C': strcat(aux_w,"-.-."); break;
            case 'D': strcat(aux_w,"-.."); break;
            case 'E': strcat(aux_w,"."); break;
            case 'F': strcat(aux_w,"..-."); break;
            case 'G': strcat(aux_w,"--."); break;
            case 'H': strcat(aux_w,"...."); break;
            case 'I': strcat(aux_w,".."); break;
            case 'J': strcat(aux_w,".---"); break;
            case 'K': strcat(aux_w,"-.-"); break;
            case 'L': strcat(aux_w,".-.."); break;
            case 'M': strcat(aux_w,"--"); break;
            case 'N': strcat(aux_w,"-."); break;
            case 'O': strcat(aux_w,"---"); break;
            case 'P': strcat(aux_w,".--."); break;
            case 'Q': strcat(aux_w,"--.-"); break;
            case 'R': strcat(aux_w,".-."); break;
            case 'S': strcat(aux_w,"..."); break;
            case 'T': strcat(aux_w,"-"); break;
            case 'U': strcat(aux_w,"..-"); break;
            case 'V': strcat(aux_w,"...-"); break;
            case 'X': strcat(aux_w,"-..-"); break;
            case 'Y': strcat(aux_w,"-.--"); break;
            case 'W': strcat(aux_w,".--"); break;
            case 'Z': strcat(aux_w,"--.."); break;
            default: return 0; //Return 0 if a character isn't found
        }
    }
}

int compare(int n, char dic[][101], char *mrs_code){
    char w_conv[101];
    int i,valid=0;

    for(i=0; i<n; i++){
        convert(dic[i],w_conv);//Converts each word of the dictionary to morse
        
        if(strncmp(mrs_code,w_conv,strlen(w_conv))==0){ 
            //If the translated word is the same as the morse code to the indicated position 
            //advances to the following line
            if (strlen(mrs_code)==strlen(w_conv)){ 
                //If the whole morse code is the same as the converted word
                valid++;         //The valid variable takes the value of valid + 1
            }else{
                valid=valid + compare(n,dic,&mrs_code[strlen(w_conv)]); //Calls the function again and does the same but from the end of the word found
            }
        }
        //If there is no correspondence continues to the next word
    }//end of the loop
    return valid; //Returns the number of valid sentences found
}
void main(){
    int n_words=0;          //number of words.
    int i;
    int s_valids=0;        //number of possible sentences
    char morse_code[1001];
    char dict[10001][101];
    scanf("%s",morse_code);// Read morse sequence.
    scanf("%d",&n_words); // Reads number of words to put in the dictionary
    for(i=0;i<n_words;i++)
        scanf("%s",dict[i]);
    //Reads all words to put in the dictionary until you reach the limit(n_words)
    s_valids=compare(n_words, dict, morse_code);//Calls the function that will compare the dictionary and the morse sequence
                                                //and places the result in s_valids.
    printf("%d/n",s_valids);  // Print number of possible sentences
}