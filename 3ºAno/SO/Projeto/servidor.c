#include <fcntl.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <time.h>
#include <unistd.h>

#define MAX 1024
#define MIX 256
typedef void (*sighandler_t) (int);

typedef struct t_struct {
  char t_time[MAX];
  char t_args[MAX];
  struct t_struct *next;
} t_struct, *task_p;

typedef struct r_struct{
  char stand_error[MAX];
  char stand_out[MAX];
  char r_args[MAX];
  int id;
  char r_time[MAX];
  char value_out[MAX];
  struct r_struct *next;
}r_struct, *exec_p;

task_p add_t(char t_time[], char t_args[], task_p task_ptr) {
  task_p new;
  if (task_ptr) {
    task_ptr -> next = add_t(t_time, t_args, task_ptr -> next);
  }
  else {
    new = (task_p) malloc(sizeof(t_struct));
    strcpy(new -> t_time, t_time);
    strcpy(new -> t_args, t_args);
    new -> next = task_ptr;
    task_ptr = new;
  }
  return task_ptr;
}

exec_p add_r(char stand_error[], char stand_out[], char r_args[],int id,char r_time[], char value_out[], exec_p task_ptr){
  exec_p new;
  new = (exec_p) malloc(sizeof(r_struct));
  if ((task_ptr)) {
    task_ptr -> next = add_r( stand_error,  stand_out,  r_args, id, r_time, value_out, task_ptr->next);
  }
  else {
    new -> id = id;
    strcpy(new -> stand_error, stand_error);
    strcpy(new -> stand_out, stand_out);
    strcpy(new -> r_args, r_args);
    strcpy(new -> r_time,r_time);
    strcpy(new -> value_out,value_out);
    new -> next = task_ptr;
    task_ptr = new;
  }
  return task_ptr;
}
void compare(int sv_cl, task_p task_ptr) {
  task_p ptr_sched = task_ptr, ptr_exec = task_ptr;
  int l = 0;
  char list[MAX], sched_s[MAX], temp_l[MAX];
  time_t sched_t = 0;
  struct tm sched;
  strcpy(list, "agendadas:\n");
  while(ptr_sched) {
    sched_t = atol(ptr_sched -> t_time);
    sched = *localtime(&sched_t);
    if (sched_t > time(NULL)) {
      sprintf(temp_l, "%d", l);
      strcat(list, temp_l);
      strcat(list, " ");
      strftime(sched_s, sizeof(sched_s), "%Y-%m-%d %H:%M:%S", &sched);
      strcat(list, sched_s);
      strcat(list, " ");
      strcat(list, ptr_sched -> t_args);
      strcat(list, "\n");
    }
    l ++;
    ptr_sched = ptr_sched -> next;
  }
  l = 0;
  strcat(list, "executadas:\n");
  while(ptr_exec) {
    sched_t = atol(ptr_exec -> t_time);
    sched = *localtime(&sched_t);
    if (sched_t < time(NULL) && sched_t > 0) {
      sprintf(temp_l, "%d", l);
      strcat(list, temp_l);
      strcat(list, " ");
      strftime(sched_s, sizeof(sched_s), "%Y-%m-%d %H:%M:%S", &sched);
      strcat(list, sched_s);
      strcat(list, " ");
      strcat(list, ptr_exec -> t_args);
      strcat(list, "\n");
    }
    l ++;
    ptr_exec = ptr_exec -> next;
  }
  write(sv_cl, list, strlen(list));
}
void sendMail(char stand_error[], char stand_out[], char r_args[],int id,char r_time[], char value_out[],char mail[]){
  int status = 0;
  char mail_body[MAX], mail_sub[MAX];
  strcpy(mail_sub, r_args);
  sprintf(mail_body, "ID:\n%d\n\nData:\n", id);
  strcat(mail_body, r_time);
  strcat(mail_body, "\n\nLinha de comandos:\n");
  strcat(mail_body,r_args);
  strcat(mail_body, "\n\nValor de saida\n");
  strcat(mail_body, value_out);
  strcat(mail_body, "\n\nStandard Output:\n");
  strcat(mail_body, stand_out);
  strcat(mail_body, "\n\nStandard Error:\n");
  strcat(mail_body, stand_error);
  strcat(mail_body, "\n");
  if (!fork()) {
    int q[2];
  	if (pipe (q) == -1){
  		perror("pipe"); exit(-1);
  	}
  	if (!fork()){
      close(q[0]);
  		dup2(q[1],1);
  		close (q[1]);
      execlp("echo", "echo", mail_body, NULL);
  		perror("erro em execlp(echo)");
      exit(-1);
  	} else {
      close (q[1]);
  		dup2(q[0],0);
  		close (q[0]);
      execlp("mail", "mail", "-s", r_args, mail, NULL);
  		perror("erro em execlp(mail), mailutilis deve estar instalado");
      exit(-1);
  	}
  }
  else {
    wait(0);
  }
}
int main() {
  task_p task_ptr = NULL;
  exec_p exec_ptr = NULL;
  int cl_sv = 0, sv_cl = 0, status, limit = 256;
  int flag = 0, size = 0, line = 0, lines = 0, diff_t = 0;
  int s = 0, i = 0, j = 0, k = 0, l = 0, a = 0, conc = 0, con = 0, sched_l[MAX];
  char c, buf[MAX], args[MAX], sched_s[MAX], lines_s[MAX], temp_l[MAX], sched_d[MIX][MAX];
  char args_next[MIX][MAX], exstat[MIX][MAX], value_out[MAX];
  char mail[MAX] = "localhost";
  time_t sched_t = time(NULL), next_t = time(NULL), temp_t = time(NULL);
  struct tm sched;
  int p[MIX][2];
  pipe(p[0]);

  int w =0;

  sighandler_t alrm () {
    char *token;
    char *arge[MAX];

    int list[conc];
    pid_t child;
    char send_output[conc+1][MAX];
    char send_args[MIX][MAX];
    char send_error[conc+1][MAX];
    char standard_output[conc+1][MAX];

    for (con = 0; con <= conc; con ++) {
      if (con < limit) {
        token = NULL;
        memset(arge, 0, sizeof(arge));
        strcpy(send_args[con],args_next[con]);
        token = strtok(args_next[con], " ");
        for (i = 0; token != NULL; i ++) {
          arge[i] = token;
          token = strtok(NULL, " ");
        }
        arge[i] = NULL;

        child = fork();
        list[con] = child;
        if (!child) {
          close(p[con][0]); // fecha tudo o que não usa!
          dup2(p[con][1],STDOUT_FILENO);
          dup2(p[con][1],STDERR_FILENO);
          execvp(arge[0], arge);
          perror("erro em execvp(tarefa)");
          exit(-1);
        }
        else {
          close(p[con][1]);

          char standard_error[MAX];
          int help=0;
          while((help=read(p[con][0],standard_output[con],MAX))>0);
          pipe(p[con+1]);
        }
      }
      else {
        task_p ptr_check = NULL;
        ptr_check = task_ptr;
        for (k = 0; k < sched_l[con] && ptr_check; k ++) {
          ptr_check = ptr_check -> next;
        }
        if (ptr_check) {
          ptr_check -> t_time[0] = '*';
        }
      }
    }
    con = 0;
      while(w=wait(&status)>0 && (con<= limit)) {
        if(WIFEXITED(status)){
          sprintf(exstat[con], "%d", WEXITSTATUS(status));
          strcpy(value_out,exstat[con]);
          if (WEXITSTATUS(status) == 0){
            strcpy(send_output[con],standard_output[con]);
            strcpy(send_error[con], "<empty>");
          }else{
            strcpy(send_error[con],standard_output[con]);
            strcpy(send_output[con], "<empty>");
          }
        }
      exec_ptr = add_r(send_error[con], send_output[con], send_args[con], sched_l[con],sched_d[con],value_out, exec_ptr);
      sendMail(send_error[con], send_output[con], send_args[con], sched_l[con],sched_d[con],value_out, mail);
      memset(send_output[con], 0, MAX);
      memset(send_error[con], 0, MAX);
      memset(standard_output[con],0,MAX);
      con++;
    }
    next_t = 17179869184;
    int full = 0;
    conc = 0;
      task_p ptr_next = task_ptr;
      for (l = 0; ptr_next != NULL; l ++) {
        temp_t = atol(ptr_next -> t_time);
        if (temp_t <= next_t && temp_t > time(NULL)) {
          if (temp_t == next_t) {
            if (conc < limit - 1) {
              conc ++;
            }
            else {
              full = 1;
            }
          }
          else {
            conc = 0;
            memset(args_next, 0, MAX);
            memset(sched_l, 0, MAX);
            memset(sched_d, 0, MAX);
          }
          if (full) {
            ptr_next -> t_time[0] = '*';
            full = 0;
          }
          else {
            strcpy(args_next[conc], ptr_next -> t_args);
            sched_l[conc] = l;
            strftime(sched_d[conc], sizeof(sched_d[conc]), "%Y-%m-%d %H:%M:%S", localtime(&temp_t));
            next_t = temp_t;
            diff_t = (int) difftime(next_t, time(NULL));
            alarm(diff_t);
          }
        }
        ptr_next = ptr_next -> next;
      }
      pipe(p[0]);
  }

  signal(SIGALRM, (sighandler_t) alrm);

  cl_sv = open("cl_sv", O_RDONLY);
  if (cl_sv < 0) {
    perror("erro em open(cl_sv)");
    exit(-1);
  }
  sv_cl = open("sv_cl", O_WRONLY);
  if (sv_cl < 0) {
    perror("erro em open(sv_cl)");
    exit(-1);
  }
  next_t = 17179869184;
  while ((size = read(cl_sv, buf, MAX)) > 0) {
    if (!strncmp("-a", buf, 2)) {
      strcpy(args, &buf[3]);
      char *time_new = strtok(args, " ");
      char *args_new = strtok(NULL, "\n");
      temp_t = atol(time_new);
      int full = 0;
      if (temp_t <= next_t && temp_t > time(NULL)) {
        if (temp_t == next_t) {
          if (conc < limit - 1) {
            conc ++;
          }
          else {
            full = 1;
          }
        }
        else {
          conc = 0;
          memset(args_next, 0, MAX);
          memset(sched_l, 0, MAX);
          memset(sched_d, 0, MAX);
        }
        if (full) {
          time_new[0] = '*';
        }
        else {
          strcpy(args_next[conc], args_new);
          sched_l[conc] = lines;
          strftime(sched_d[conc], sizeof(sched_d[conc]), "%Y-%m-%d %H:%M:%S", localtime(&temp_t));
          next_t = temp_t;
          diff_t = (int) difftime(next_t, time(NULL));
          alarm(diff_t);
        }
      }
      if (full) {
        strcpy(lines_s, "comando ou argumento invalido");
      }
      else {
        sprintf(lines_s, "%d", lines);
        task_ptr = add_t(time_new, args_new, task_ptr);
        lines ++;
      }
      write(sv_cl, lines_s, strlen(lines_s));
    }
    else if (!strncmp("-l", buf, 2)) {
      compare(sv_cl, task_ptr);
    }
    else if(!strncmp("-r",buf,2)){
      char details[MAX];
      int found = 0;
      int wanted = atoi(&buf[3]);
      exec_p mais_um = NULL;
      mais_um = exec_ptr;
      while(mais_um){
        if(mais_um->id == wanted){
          found = 1;
          sprintf(details, "ID:\n%d\n\nData:\n", mais_um -> id);
          strcat(details, mais_um -> r_time);
          strcat(details, "\n\nLinha de comandos:\n");
          strcat(details, mais_um -> r_args);
          strcat(details, "\n\nValor de saida\n");
          strcat(details, mais_um -> value_out);
          strcat(details, "\n\nStandard Output:\n");
          strcat(details, mais_um -> stand_out);
          strcat(details, "\n\nStandard Error:\n");
          strcat(details, mais_um -> stand_error);
          strcat(details, "\n");
        }
        mais_um = mais_um->next;
      }
      if (!found) {
        strcpy(details, "comando ou argumento invalido");
      }
      write(sv_cl, details, strlen(details));
    }
    else if (!strncmp("-n", buf, 2)) {
      limit = atoi(&buf[3]);
    }
    else if(!strncmp("-e", buf, 2)){
      memset(mail, 0, sizeof(mail));
      strcpy(mail, &buf[3]);
    }
    else if (!strncmp("-c", buf, 2)) {
      l = atoi(&buf[3]);
      task_p ptr_check = task_ptr;
      for (k = 0; k < l && ptr_check; k ++) {
        ptr_check = ptr_check -> next;
      }
      if (ptr_check && atoi(ptr_check -> t_time) > time(NULL)) {
        ptr_check -> t_time[0] = '*';
        next_t = 17179869184;
        task_p ptr_resched = task_ptr;
        int full = 0;
        for (l = 0; ptr_resched != NULL; l ++) {
          temp_t = atol(ptr_resched -> t_time);
          if (temp_t <= next_t && temp_t > time(NULL)) {
            if (temp_t == next_t) {
              if (conc < limit - 1) {
                conc ++;
              }
              else {
                full = 1;
              }
            }
            else {
              conc = 0;
              memset(args_next, 0, MAX);
              memset(sched_l, 0, MAX);
              memset(sched_d, 0, MAX);
            }
            if (full) {
              ptr_resched -> t_time[0] = '*';
              full = 0;
            }
            else {
              strcpy(args_next[conc], ptr_resched -> t_args);
              sched_l[conc] = l;
              strftime(sched_d[conc], sizeof(sched_d[conc]), "%Y-%m-%d %H:%M:%S", localtime(&temp_t));
              next_t = temp_t;
              diff_t = (int) difftime(next_t, time(NULL));
              alarm(diff_t);
            }
          }
          ptr_resched = ptr_resched -> next;
        }
      }
      else {
        write(sv_cl, "comando ou argumento invalido", 29);
      }
    }
    memset(buf, 0, sizeof(buf));
  }
  close(cl_sv);
  close(sv_cl);
  return 0;
}
