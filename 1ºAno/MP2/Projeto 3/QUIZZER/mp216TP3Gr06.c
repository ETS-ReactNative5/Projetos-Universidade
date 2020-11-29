//  mp216TP3Gr06.c
//  TP3 | Jogo Quiz

//  Grupo 6 - PL1
//  Leandro Alves       | A82157
//  J. Eduardo Santos   | A82350
//  David Machado       | A82381

//  Versao final 1.0

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <time.h>
#include <unistd.h>
#define MAX 150

typedef struct stat_s
{
    char name [MAX];
    int correct [6];
    int defeated [8];
    int lv [3];
    int streak;
}   stat_t;

typedef struct pwrup_s
{
    char name [MAX];
    char abrev [MAX];
    char desc [MAX];
    float use;
}   pwrup_t;

typedef struct abil_s
{
    char name [MAX];
    char desc [MAX];
    int use;
}   abil_t;

typedef struct elite_s
{
    char cat [MAX];
    char name [6] [MAX];
    int use;
}   elite_t;

typedef struct minion_s
{
    char cat [MAX];
    char name [10] [MAX];
}   minion_t;

typedef struct wrong_s
{
    char option [MAX];
    struct wrong_s *next;
}   wrong_t, *wrong_p;

typedef struct quest_s
{
    char question [MAX];
    char correct [MAX];
    struct quest_s *next;
    struct wrong_s *answer;
}   quest_t, *quest_p;

void Stats_Read (stat_t *stat)
{
    int i = 0;
    char temp [MAX];

    FILE *file;

    file = fopen ("statistics.txt", "r");

    if (file)
    {
        fgets (stat -> name, MAX, file);       
        stat -> name [strlen (stat -> name) - 1] = '\0';

        for (i = 0; i < 6; i ++)
        {
            fgets (temp, MAX, file);
            stat -> correct [i] = atoi (temp);
        }
        for (i = 0; i < 8; i ++)
        {
            fgets (temp, MAX, file);
            stat -> defeated [i] = atoi (temp);
        }
        for (i = 0; i < 3; i ++)
        {
            fgets (temp, MAX, file);
            stat -> lv [i] = atoi (temp);
        }
        fgets (temp, MAX, file);
        stat -> streak = atoi (temp);

        fclose (file);
    }
}
void Pwrups_Read (pwrup_t *pwrup)
{
    int i = 0;
    char temp [MAX];

    FILE *file;

    file = fopen ("powerups.txt", "r");

    if (file)
    {
        while (!feof (file))
        {
            fgets (pwrup[i].name, MAX, file);
            pwrup[i].name [strlen (pwrup[i].name) - 1] = '\0';

            fgets (pwrup[i].abrev, MAX, file);
            pwrup[i].abrev [strlen (pwrup[i].abrev) - 1] = '\0';

            fgets (pwrup[i].desc, MAX, file);
            pwrup[i].desc [strlen (pwrup[i].desc) - 1] = '\0';

            fgets (temp, MAX, file);
            pwrup[i].use = atoi (temp);

            i ++;
        }
        fclose (file);
    }
}
void Abils_Read (abil_t *abil)
{
    int i = 0;
    char temp [MAX];

    FILE *file;

    file = fopen ("abilities.txt", "r");

    if (file)
    {
        while (!feof (file))
        {
            fgets (abil[i].name, MAX, file);
            abil[i].name [strlen (abil[i].name) - 1] = '\0';

            fgets (abil[i].desc, MAX, file);
            abil[i].desc [strlen (abil[i].desc) - 1] = '\0';

            fgets (temp, MAX, file);
            abil[i].use = atoi (temp);

            i ++;
        }
        fclose (file);
    }
}
void Elite_Read (elite_t *elite)
{
    int i = 0, j;
    char temp [MAX];

    FILE *file;

    file = fopen ("elite.txt", "r");

    if (file)
    {
        while (!feof (file))
        {
            fgets (elite[i].cat, MAX, file);
            elite[i].cat [strlen (elite[i].cat) - 1] = '\0';

            for (j = 0; j < 6; j ++)
            {
                fgets (elite[i].name[j], MAX, file);
                elite[i].name[j] [strlen (elite[i].name[j]) - 1] = '\0';
            }
            fgets (temp, MAX, file);
            elite[i].use = atoi (temp);

            i ++;
        }
        fclose (file);
    }
}
void Minions_Read (minion_t *minion)
{
    int i = 0, j;
    char temp [MAX];

    FILE *file;

    file = fopen ("minions.txt", "r");

    if (file)
    {
        while (!feof (file))
        {
            fgets (minion[i].cat, MAX, file);
            minion[i].cat [strlen (minion[i].cat) - 1] = '\0';

            for (j = 0; j < 10; j ++)
            {
                fgets (minion[i].name[j], MAX, file);
                minion[i].name[j] [strlen (minion[i].name[j]) - 1] = '\0';
            }
            i ++;
        }
        fclose (file);
    }
}
quest_p Insert (quest_p head, quest_p new)
{
    if (head)
    {
        if (strcmp (head -> question, new -> question) > 0)   
        {
            head -> next = Insert (head -> next, new);

        }
        else
        {
            new -> next = head;
            head = new;           
        }
    }
    else
    {
       head = new;
    }
    return head;
}
quest_p Quests_Read (int cat)
{
    int temp_cat, i = 0, count = 0, selected;
    char temp [MAX], question [MAX], answer [MAX];

    quest_p head = NULL, new = NULL;
    wrong_p wrong_new = NULL, wrong_head = NULL;

    FILE *file;

    file = fopen ("questions.txt", "r");

    if (file)
    { 
        while (fgets (temp, MAX, file))
        {
            temp_cat = atoi (temp);

            if (temp_cat == cat)
            {
                new = (quest_p) malloc (sizeof (quest_t));

                fgets (new -> question, MAX, file);
                fgets (new -> correct, MAX, file);

                for (i = 0; i < 3; i ++)
                {
                    wrong_new = (wrong_p) malloc (sizeof (wrong_t));

                    fgets (wrong_new -> option, MAX, file);
                    wrong_new -> next = wrong_head;
                    wrong_head = wrong_new;
                }
                new -> answer = wrong_new;

                head = Insert (head, new);

                count ++;
            }
            else
            {
                for (i = 0; i < 5; i ++)
                {
                    fgets (temp, MAX, file);
                }
            }
        }
        selected = rand () %count;

        i = 0;

        while (selected != i)
        {
            head = head -> next;
            i ++;
        }
    }
    fclose (file);

    return head;
}
void Stats_Write (stat_t *stat)
{
    int i;

    FILE *file;

    file = fopen ("statistics.txt", "w");
    
    fprintf (file, "%s\n", stat -> name);

    for (i = 0; i < 6; i ++)
    {
        fprintf (file, "%d\n", stat -> correct [i]);
    }
    for (i = 0; i < 8; i ++)
    {
        fprintf (file, "%d\n", stat -> defeated [i]);
    }
    for (i = 0; i < 3; i ++)
    {
        fprintf (file, "%d\n", stat -> lv [i]);
    }
    fprintf (file, "%d\n", stat -> streak);

    fclose (file);
}
int Powerups (int error, pwrup_t *pwrup, abil_t *abil)
{
    int i, used_pwrups = 0, avail_pwrups = 0, n;
    char temp [MAX];
    
    printf ("[Powerups]\n");

    for (i = 0; i < 7; i ++)
    {
        if (pwrup[i].use == 1)
        {
            used_pwrups ++;
        }
        else
        {
            avail_pwrups ++;
        }
    }
    if (used_pwrups > 0)
    {
        if (pwrup[0].use == 0)
        {
            printf ("\n[In use]\n\n");
        }
        else
        {
            if (used_pwrups > 1)
            {
                printf ("\n[In use / already used]\n\n");
            }
            else
            {
                printf ("\n[Already used]\n\n");
            }
        }
        for (i = 0; i < 7; i ++)
        {
            if (pwrup[i].use == 1)
            {
                printf ("* %s (%s) - %s\n", pwrup[i].name, pwrup[i].abrev, pwrup[i].desc);
            }
        }
    }
    if (avail_pwrups > 0)
    {    
        printf ("\n[Available]\n\n");

        for (i = 0; i < 7; i ++)
        {
            if (pwrup[i].use == 0)
            {
                printf ("%d. %s (%s) - %s\n", i + 1, pwrup[i].name, pwrup[i].abrev, pwrup[i].desc);
            }
        }
    }
    if (error == 0)
    {
        printf ("\n> Number: ");
    }
    else
    {
        printf ("\n> Please, choose a valid number: ");
    }
    fgets (temp, sizeof (temp), stdin);
    n = atoi (temp);

    if (n == 1 && pwrup[n - 1].use == 0)
    {
        for (i = 0; i < 4; i ++)
        {
            abil[i].use ++;
        }
    }
    pwrup[n - 1].use ++;

    if (n < 1 || n > 7 || pwrup[n - 1].use == 2)
    {
        error = 1;

        if (pwrup[n - 1].use == 2)
        {
        	pwrup[n - 1].use = 1;
        }
    }
    else
    {
        error = 0;
    }
    return error;
}
int Options (int using_abil, int play, abil_t *abil, quest_p quest)
{
    int i, j, correct, k, used_abils = 0, array [4] = {0, 0, 0, 0};

    wrong_p wrong;

    for (i = 1; i <= 4; i ++)
    {
        j = rand () %4;
        while (array [j] == 1)
        {
            j = rand () %4;
        }
        array [j] = 1;
        if (j == 0)
        {
            printf ("%d. %s", i, quest -> correct);
            correct = i;
        }
        else
        {
            k = 1;
            wrong = quest -> answer;
            while (k < j)
            {
                wrong = wrong -> next;
                k ++;
            }
            printf ("%d. %s", i, wrong -> option);
        }
    }
    for (i = 0; i < 4; i ++)
    {
        if (abil[i].use > 0)
        {
            used_abils ++;
        }
    }
    if (used_abils > 0 && using_abil == 0 && play == 1)
    {
        printf ("\n[Abilities]\n\n");

        for (i = 0; i < 4; i ++)
        {
            if (abil[i].use > 0)
            {
                printf ("%d. %s (%d) - %s\n", i + 5, abil[i].name, abil[i].use, abil[i].desc);
            }
        }
    }
    if (using_abil == 0)
    {
        printf ("\n0. Give up\n");
    }
    return correct;
}
int Bomb (int correct, abil_t *abil)
{
    int i, answer, bomb [2];
    char temp [MAX];

    for (i = 0; i < 2; i ++)
    {
        bomb [i] = (rand () %3) + 1;

        while (bomb [i] == correct || bomb [0] == bomb [1])
        {
            bomb [i] = (rand () %3) + 1;
        }
    }
    printf ("\n[Bomb]\n\n\"The options %d. and %d. have exploded, these are totally wrong!\"\n\n", bomb [0], bomb [1]);

    printf("> Answer: ");
    fgets (temp, MAX, stdin);
    answer = atoi (temp);

    if (answer == correct)
    {
        correct = 1;
    }
    else
    {
        correct = 2;
    }
    return correct;
}
int Double_Chance (int correct, abil_t *abil)
{
    int stop = 0, i = 0, answer;
    char temp [MAX];

    printf ("\n[Double Chance]\n\n");

    while (stop == 0 && i < 2)
    {    
        if (i == 0)
        {
            printf ("> Answer: ");
        }
        else
        {
            printf ("\n> Try again: ");                   
        }
        fgets (temp, MAX, stdin);
        answer = atoi (temp);

        if (answer == correct)
        {
            stop = 1;
        }
        i ++;
    }
    if (stop == 1)
    {
        correct = 1;
    }
    else
    {
        correct = 2;
    }
    return correct;
}
int Skip (int cat, int play, abil_t *abil)
{
    int correct, answer;
    char temp [MAX];

    quest_p quest;

    printf ("\n[Skip]\n\n");

    quest = Quests_Read (cat);
    printf ("%s\n", quest -> question);

    correct = Options (1, play, abil, quest);

    printf ("\n> Answer: ");
    fgets (temp, MAX, stdin);
    answer = atoi (temp);

    if (answer == correct)
    {
        correct = 1;
    }
    else
    {
        correct = 2;
    }
    return correct;
}
int Summon_Ally (int correct, abil_t *abil)
{
    int opinion, answer;
    char temp [MAX];

    if (rand () %10 < 7)
    {
        opinion = correct;
    }
    else
    {
        opinion = (rand () %3) + 1;

        while (opinion == correct)
        {
            opinion = (rand () %3) + 1;
        }
    }
    printf ("\n[Ally]\n\n\"I think the correct answer is the option %d.!\"\n\n", opinion);

    printf("> Answer: ");
    fgets (temp, MAX, stdin);
    answer = atoi(temp);

    if (answer == correct)
    {
        correct = 1;
    }
    else
    {
        correct = 2;
    }
    return correct;
}
int Elite (int lv, int error, stat_t *stat, elite_t *elite)
{
    int i, used_elite = 0, avail_elite = 0, n;
    char temp [MAX];

    printf ("[Opponents]\n");

    for (i = 0; i < 6; i ++)
    {
        if (elite[i].use == 1)
        {
            used_elite ++;
        }
        else
        {
            avail_elite ++;
        }
    }
    if (used_elite > 0)
    {
        printf ("\n[Defeated]\n\n");

        for (i = 0; i < 6; i ++)
        {
            if (elite[i].use == 1)
            {
                printf ("* %s (%d pts)\n", elite[i].cat, stat -> correct [i]);
            }
        }
    }
    if (avail_elite > 0)
    {
        printf ("\n[Available]\n\n");

        for (i = 0; i < 6; i ++)
        {
            if (elite[i].use == 0)
            {
                printf ("%d. %s (%s) (%d pts)\n", i + 1 , elite[i].name[lv], elite[i].cat, stat -> correct [i]);
            }
        }
    }
    if (error == 0)
    {
        printf ("\n> Number: ");
    }
    else
    {
        printf ("\n> Please, choose a valid number: ");
    }
    fgets (temp, sizeof (temp), stdin);
    n = atoi (temp);

    system ("clear");
    return (n - 1);
}
int Questions (int choose, int play, abil_t *abil)
{
    int correct, answer = 0;
    char temp [MAX];

    quest_p quest = NULL;

    quest = Quests_Read (choose);

    printf("(!) %s\n", quest -> question);

    correct = Options (0, play, abil, quest);
        
    printf ("\n> Answer: ");
    fgets (temp, MAX, stdin);
    answer = atoi (temp);

    switch (answer)
    {
        case 0: correct = 0;
                break;

        case 5: if (abil[0].use > 0)
                {
                    abil[0].use --;
                    correct = Bomb (correct, abil);
                }
                else
                {
                	correct = 2;
                }
                break;

        case 6: if (abil[1].use > 0)
                {
                    abil[1].use --;
                    correct = Double_Chance (correct, abil);
                }
                else
                {
                	correct = 2;
                }
                break;

        case 7: if (abil[2].use > 0)
                {
                    abil[2].use --;
                    correct = Skip (choose, play, abil);
                }
                else
                {
                	correct = 2;
                }
                break;

        case 8: if (abil[3].use > 0)
                {
                    abil[3].use --;
                    correct = Summon_Ally (correct, abil);
                }
                else
                {
                	correct = 2;
                }
                break;

        default: if (answer == correct)
                {
                    correct = 1;
                }
                else
                {
                    correct = 2;
                }
                break;
    }
    return correct;
}
void Status (int hp_p, int hp_e, int hp_e_f, int lv, int choose, int play, int atk_p, int streak, int atk_e, char name_e [], char cat [], stat_t *stat, pwrup_t *pwrup)
{
    int i, j;

    system ("clear");

    if (hp_p > 100)
    {
        hp_p == 100;
    }
    if (hp_e > hp_e_f)
    {
        hp_e == hp_e_f;
    }
    printf ("\n< %s >", stat -> name);

    for (i = 0; i != 21 - strlen (stat -> name); i ++)
    {
        printf (" ");
    }
    if (play == 1)
    {
        printf ("\t[Elite Six]\t");
    }
    else if (play == 2)
    {
        printf ("\t[Survival]\t");
    }
    else 
    {
        printf ("\t[Hardcore]\t");
    }
    if (choose != -1)
    {
        printf ("< %s", name_e);

        if (choose != -2)
        {
            printf (" | %s (%d pts)", cat, stat -> correct [choose]);
        }
        printf (" >");
    }
    printf ("\n[HP: ");

    for (j = 0; j <= 10; j ++)
    {
        if (hp_p >= j * 10 && hp_p < (j + 1) * 10)
        {
            if (hp_p >= 1 && hp_p <= 10)
            {
                i = 0;
                j = 1;
            }
            for (i = 0; i < j; i ++)
            {
                printf ("#");
            }
            for (i = j; i < 10; i ++)
            {
                printf ("-");
            }
        }
    }
    printf ("] (%d/100)", hp_p);
    
    if (play != 2)
    {
        printf ("\t[Level %d]\t", lv);
    }
    else
    {
        printf ("\t[Round %d]\t", lv);
    }
    if (choose != -1)
    {
        printf ("[HP: ");

        for (j = 0; j <= 10; j ++)
        {
            if (hp_e >= j * hp_e_f / 10 && hp_e < (j + 1) * hp_e_f / 10)
            {
                if (hp_e >= 1 && hp_e <= hp_e_f / 10)
                {
                    i = 0;
                    j = 1;
                }
                for (i = 0; i < j; i ++)
                {
                    printf ("#");
                }
                for (i = j; i < 10; i ++)
                {
                    printf ("-");
                }
            }
        }
        printf ("] (%d/%d)", hp_e, hp_e_f);
    }
    printf ("\n(ATK: %d)", atk_p);

    printf ("\t\t\t[Streak: %d]\t", streak);

    if (choose != -1)
    {
        printf ("(ATK: %d)", atk_e);
    }
    printf ("\n");

    for (i = 1; i < 7; i ++)
    {
        if (pwrup[i].use == 1)
        {
            printf ("(%s) ", pwrup[i].abrev);
        }
    }
    printf("\n\n");
}
int Play (int play, stat_t *stat, pwrup_t *pwrup, abil_t *abil, elite_t *elite, minion_t *minion)
{
    int i, finish = 0, lv = 1, streak = 0, choose = -1, error, acc_p, acc_e, crit, correct;
    float atk_p, hp_p = 100, hp_e, hp_e_f, atk_e, dmg_e, dmg_p;
    char name_e [MAX], cat [MAX];

    for (i = 0; i < 4; i ++)
    {
        if (play == 1)
        {
            abil[i].use = 1;
        }
        else
        {
            abil[i].use = 0;
        }
    }
    for (i = 0; i < 7; i ++)
    {
        pwrup[i].use = 0;
    }
    for (i = 0; i < 6; i ++)
    {
        elite[i].use = 0;
    }
    while (finish == 0)
    {
        atk_p = 20 + 10 * (pwrup[5].use);
        Status (hp_p, hp_e, hp_e_f, lv, choose, play, atk_p, streak, atk_e, name_e, cat, stat, pwrup);
        
        if (play != 2 && lv < 7)
        {
            choose = Elite (lv - 1, 0, stat, elite);
            while (choose < 0 || choose > 5 || elite[choose].use == 1)
            {
                choose = -1;
                Status (hp_p, hp_e, hp_e_f, lv, choose, play, atk_p, streak, atk_e, name_e, cat, stat, pwrup);
                choose = Elite (lv - 1, 1, stat, elite);
            }
            elite[choose].use = 1;
            strcpy (name_e, elite[choose].name [lv - 1]);
            strcpy (cat, elite[choose].cat);
        }
        hp_e_f = 75 + 25 * lv;
        hp_e = hp_e_f;

        atk_e = 10 + 5 * lv;

        if (play == 2)
        {
            choose = rand () %6;
            hp_e_f = 10;
            hp_e = 10;
            strcpy (name_e, minion[choose].name [rand () %10]);
            strcpy (cat, minion[choose].cat);
        }
        if (play != 2 && lv == 7)
        {
            hp_e_f = 300;
            hp_e = 300;
            atk_e = 50;
            strcpy (name_e, "Champion EdKeriohn");
            choose = -2;
        }
        Status (hp_p, hp_e, hp_e_f, lv, choose, play, atk_p, streak, atk_e, name_e, cat, stat, pwrup);
        
        if (play == 1)
        {
            error = Powerups (0, pwrup, abil);
            while (error == 1)
            {
                Status (hp_p, hp_e, hp_e_f, lv, choose, play, atk_p, streak, atk_e, name_e, cat, stat, pwrup);
                error = Powerups (1, pwrup, abil);
            }
        }
        atk_p = 20 + 10 * (pwrup[5].use);
        if (play == 2)
        {
            pwrup[1].use = 1;
            pwrup[3].use = 1;
            pwrup[4].use = 1;
            pwrup[6].use = 1;
        }
        while (hp_p >= 1 && hp_e >= 1)
        {
            if (pwrup[1].use == 0)
            {
                acc_p = rand () %5;
            }
            else
            {
                acc_p = 1;
            }
            acc_e = rand () %5;

            if (rand () %10 == 2)
            {
                crit = 2;
            }
            else
            {
                crit = 1;
            }
            dmg_e = atk_p * crit * ((rand () %16) + 85) / 100;

            if (pwrup[4].use == 0)
            {
                dmg_p = atk_e * crit * ((rand () %16) + 85) / 100;
            }
            else
            {
                dmg_p = 2 * atk_e / 3 * crit * ((rand () %16) + 85) / 100;
            }
            if (play != 2 && lv == 7)
            {
                choose = rand () %6;
                strcpy (cat, elite[choose].cat);
            }
            Status (hp_p, hp_e, hp_e_f, lv, choose, play, atk_p, streak, atk_e, name_e, cat, stat, pwrup);

            correct = Questions (choose, play, abil);
            if (correct == 1)
            {
                sleep (2);
                printf ("\n(!) Correct!\n\n");
                streak ++;
                if (streak > stat -> streak)
                {
                    stat -> streak = streak;
                }
                stat -> correct [choose] ++;
                sleep (2);
                Status (hp_p, hp_e, hp_e_f, lv, choose, play, atk_p, streak, atk_e, name_e, cat, stat, pwrup);
                sleep (2);
                if (acc_p != 0)
                {
                    hp_e -= dmg_e;
                    if (hp_e < 0)
                    {
                        hp_e = 0;
                    }
                    hp_p += 0.5 * dmg_e * pwrup[6].use;
                    if (hp_p > 100)
                    {
                        hp_p = 100;
                    }
                    if (crit == 2)
                    {
                        printf ("(!) Critical hit!\n\n");
                        sleep (2);
                    }
                    Status (hp_p, hp_e, hp_e_f, lv, choose, play, atk_p, streak, atk_e, name_e, cat, stat, pwrup);
                    if (crit == 2)
                    {
                        printf ("(!) Critical hit!\n\n");
                    }
                    printf ("(!) You took %.1f HP!\n\n", dmg_e);
                    sleep (3);
                }
                else
                {
                    printf ("(!) Attack missed...\n\n");
                    sleep (3);
                }
            }
            else if (correct == 2)
            {
                sleep (2);
                printf ("\n(!) Wrong!\n\n");
                streak = 0;
                sleep (2);
                Status (hp_p, hp_e, hp_e_f, lv, choose, play, atk_p, streak, atk_e, name_e, cat, stat, pwrup);
                sleep (2);
                if (acc_e != 0)
                {
                    hp_p -= dmg_p;
                    if (hp_p < 0)
                    {
                        hp_p = 0;
                    }
                    if (crit == 2)
                    {
                        printf ("(!) Critical hit!\n\n");
                        sleep (2);
                    }
                    Status (hp_p, hp_e, hp_e_f, lv, choose, play, atk_p, streak, atk_e, name_e, cat, stat, pwrup);
                    if (crit == 2)
                    {
                        printf ("(!) Critical hit!\n\n");
                    }
                    printf ("(!) You lost %.1f HP!\n\n", dmg_p);
                    sleep (3);
                }
                else
                {
                    printf ("(!) Attack missed...\n\n");
                    sleep (3);
                }
            }
            else
            {
                hp_p = 0;
            }
            if (hp_e == 0)
            {
                Status (hp_p, hp_e, hp_e_f, lv, choose, play, atk_p, streak, atk_e, name_e, cat, stat, pwrup);
                printf ("(!) You have defeated %s!\n\n", name_e);
                if (play != 2)
                {
                    if (lv == 7)
                    {
                        stat -> defeated [6] ++;
                    }
                    else
                    {
                        stat -> defeated [lv - 1] ++;
                    }
                }
                else
                {
                    stat -> defeated [7] ++;
                }
                sleep (3);

                if (play != 2 && lv == 7)
                {
                    printf ("(!) Congratulations! You are now the Champion of the Elite Six!\n\n");
                    sleep (3);
                    finish = 1;
                }
            }
            if (hp_p < 1)
            {
                Status (hp_p, hp_e, hp_e_f, lv, choose, play, atk_p, streak, atk_e, name_e, cat, stat, pwrup);
                printf ("(!) You have been defeated by %s...\n\n", name_e);
                sleep (3);
                printf ("(!) Game over!\n\n");
                sleep (3);
                hp_p = 0;
                finish = 1;
            }
            else
            {
                hp_p += 5 * pwrup[3].use;
                if (hp_p > 100)
                {
                    hp_p = 100;
                }
            }
        }
        if (hp_p > 0)
        {
            hp_p += 50 * pwrup[2].use;
        }
        if (hp_p > 100)
        {
            hp_p = 100;
        }
        if (lv - 1 > stat -> lv [play - 1])
        {
            stat -> lv [play - 1] = lv - 1;
        }
        lv ++;
    }
}
int New_Game (stat_t *stat, pwrup_t *pwrup, abil_t *abil, elite_t *elite, minion_t *minion)
{
    int n = -1, error = 0;
    char temp [MAX];

    while (n != 0)
    {    
        system ("clear");

        printf ("\n[New game]\n\n");

        printf ("[Game modes]\n\n");

        printf ("1. Elite Six:\n");
        printf ("   Defeat each member of the Elite Six, each one specialized in a category.\n");
        printf ("   If you defeat all of them, you may challenge the Champion of the Elite specialized in every category.\n\n");
        printf ("2. Survival:\n");
        printf ("   Face an infinite wave of Minions and stay alive for the longest number of rounds.\n");
        printf ("   You have some unlocked power-ups with you but you can't use abilities.\n\n");
        printf ("3. Hardcore:\n");
        printf ("   Elite Six game mode with no power-ups nor abilities.\n\n");

        printf ("0. Cancel.\n\n");

        if (error == 0)
        {
            printf ("> Number: ");
        }
        else
        {
            printf ("> Please, choose a valid number: ");
        }
        fgets (temp, sizeof(temp), stdin);
        n = atoi (temp);

        if (n >= 1 && n <= 3)
        {
            Play (n, stat, pwrup, abil, elite, minion);
        }
        else
        {
            error = 1;
        }
    }
    system ("clear");
}
void Name (stat_t *stat)
{
    char name_temp [MAX];

    system ("clear");

    printf ("\n[Changing name]\n\n");
    printf ("> Name: ");
    fgets (name_temp, sizeof (name_temp), stdin);
    name_temp [strlen (name_temp) - 1] = '\0';

    while (strlen (name_temp) > 21)
    {
        system ("clear");
        printf ("\n[Changing name]\n\n");
        printf ("> Your name must have less than 21 characters: ");
        fgets (name_temp, sizeof (name_temp), stdin);
        name_temp [strlen (name_temp) - 1] = '\0';
    }
    strcpy (stat -> name, name_temp);

    system ("clear");
}
void Delete (stat_t *stat)
{
    int i;

    for (i = 0; i < 6; i ++)
    {
        stat -> correct [i] = 0;
    }
    for (i = 0; i < 8; i ++)
    {
        stat -> defeated [i] = 0;
    }
    for (i = 0; i < 3; i ++)
    {
        stat -> lv [i] = 0;
    }
    stat -> streak = 0;

    system ("clear");
}
void Statistics (int error, stat_t *stat, elite_t *elite)
{
    int i, correct = 0, defeated = 0, n;
    char temp [MAX];

    system ("clear");

    printf ("\n[Statistics]\n\n");

    printf ("[Name]: %s\n\n", stat -> name);

    for (i = 0; i < 6; i ++)
    {
        correct += stat -> correct [i];
    }
    printf ("[Correct answers]: %d\n\n", correct);

    for (i = 0; i < 6; i ++)
    {
        printf ("* %s: %d\n", elite[i].cat, stat -> correct [i]);
    }
    for (i = 0; i < 8; i ++)
    {
        defeated += stat -> defeated [i];
    }
    printf ("\n[Defeated opponents]: %d\n\n", defeated);

    printf ("* Elite (1-6): %d", stat -> defeated [0]);
    for (i = 1; i < 6; i ++)
    {
        printf (", %d", stat -> defeated [i]);
    }
    printf ("\n* Champion: %d\n", stat -> defeated [i]);
    printf ("* Minions: %d\n\n", stat -> defeated [i + 1]);

    printf ("[Game modes (records)]\n\n");

    printf ("* Elite Six: level %d", stat -> lv [0]);
    if (stat -> lv [0] == 7)
    {
        printf (" (completed)");
    }
    printf ("\n* Survival: %d rounds\n", stat -> lv [1]);
    printf ("* Hardcore: level %d", stat -> lv [2]);
    if (stat -> lv [2] == 7)
    {
        printf (" (completed)");
    }
    printf ("\n\n[Biggest correct answer-streak]: %d\n\n", stat -> streak);

    printf ("1. Change name\n");
    printf ("2. Delete progress\n\n");
    printf ("0. Cancel\n\n");

    if (error == 0)
    {
        printf ("> Number: ");
    }
    else
    {
        printf ("> Please, choose a valid number: ");
    }
    fgets (temp, sizeof (temp), stdin);
    temp [strlen (temp) - 1] = '\0';
    n = atoi (temp);

    switch (n)
    {
        case 0: system ("clear");
                break;

        case 1: Name (stat);
                break;

        case 2: Delete (stat);
                break;

        default: Statistics (1, stat, elite);
                 break;
    }
}
void Credits ()
{
    system ("clear");
    printf ("\n[Credits]\n\n");
    sleep (1);
    printf ("< The QUIZZER by EdKeriohn v.1.0 >\n\n");
    sleep (1);
    printf ("[Developer team]\n\n");
    printf ("* Leandro Alves - A82157\n");
    printf ("* J. Eduardo Santos - A82350\n\n");
    sleep (3);
    printf ("[Former member]\n\n");
    printf ("* David Machado - A82381\n\n");
    sleep (2);
    printf ("[Software used]\n\n");
    printf ("* Sublime Text - Code writting (C)\n");
    printf ("* Texmaker - Report writting (LaTeX)\n");
    printf ("* ubuntu 16.04 LTS - Operative system\n\n");
    sleep (4);
    printf ("[Based on]\n\n");
    printf ("* Pokemon - Combat mechanics and Elite Six game mode\n");
    printf ("* Team Fortress 2 - Power-ups\n");
    printf ("* Trivia Crack - Categories, abilities and questions\n\n");
    sleep (4);
    printf ("[Development time]\n\n");
    printf ("* 2 weeks\n");
    printf ("* Release date: June 11th, 2017\n\n");
    sleep (3);
    printf ("[Other]\n\n");
    printf ("* Metodos de Programacao II\n");
    printf ("* Mestrado Integrado em Engenharia de Telecomunicacoes e Informatica\n");
    printf ("* University of Minho\n\n");
    sleep (4);
    printf ("< EdKeriohn 2017 >\n\n");
    sleep (3);
    system ("clear");
}
int main ()
{
    int n = -1, error = 0;
    char temp [MAX];

    stat_t stat;
    pwrup_t pwrup [MAX] = {0};
    abil_t abil [MAX] = {0};
    elite_t elite [MAX] = {0};
    minion_t minion [MAX] = {0};

    Stats_Read (&stat);
    Pwrups_Read (pwrup);
    Abils_Read (abil);
    Elite_Read (elite);
    Minions_Read (minion);

    srand (time (NULL));

    system ("clear");

    printf ("\n  (!) Hi and welcome to The QUIZZER by EdKeriohn.\n");
    printf ("(!) After one month in development, it is worth the wait.\n");
    printf ("\t    (!) Thanks and have fun!\n");

    while (n != 0)                         
    {
        printf ("\n");
        printf ("****** #### #  # ### ##### ##### #### #### ******\n");
        printf (" ***** #  # #  #  #  #   # #   # #    #  # *****\n");
        printf ("  **** #  # #  #  #     ##    ## #### #  # ****\n");
        printf ("   *** #  # #  #  #  ###   ###   #    ###  ***\n");
        printf ("    ** ###  #  #  #  #   # #   # #    #  # **\n");
        printf ("     *   ##  ##  ### ##### ##### #### #  # *\n\n");

        printf ("\t\t [Main Menu]\n\n");

        printf ("\t\t 1. New game\n");
        printf ("\t\t2. Statistics\n");
        printf ("\t\t 3. Credits\n\n");
        printf ("\t\t  0. Exit\n\n");

        if (error == 0)
        {
            printf ("\t\t > Number: ");
        }
        else
        {
            printf ("\t> Please, choose a valid number: ");
        }
        fgets (temp, sizeof (temp), stdin);
        n = atoi (temp);

        switch (n)
        {
            case 1: New_Game (&stat, pwrup, abil, elite, minion);
            		error = 0;
                    break;

            case 2: Statistics (0, &stat, elite);
            		error = 0;
                    break;

            case 3: Credits ();
            		error = 0;
                    break;

            default: error = 1;
                     system ("clear");
                     break;
        }
    }
    system ("clear");
    printf ("\n(!) Thank you for playing The QUIZZER by EdKeriohn!\n");
    printf ("\n(!) Please, come back again! ;)\n\n");
    sleep (4);
    system ("clear");
    Stats_Write (&stat);

    return 0;
}