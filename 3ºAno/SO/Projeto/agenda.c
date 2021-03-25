#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <time.h>
#include <unistd.h>

#define MAX 1024

int main(int argc, char **argv) {
  int cl_sv = 0, sv_cl = 0;
  int size = 0, stop = 0;
  int i = 0, j = 0;
  char buf[MAX], args[MAX], sched_s[MAX];
  char year[MAX], month[MAX], day[MAX], hour[MAX], min[MAX], sec[MAX];
  time_t sched_t = time(NULL);
  struct tm sched;

  cl_sv = open("cl_sv", O_WRONLY);
  if (cl_sv < 0) {
    perror("erro em open(cl_sv)");
    exit(-1);
  }
  sv_cl = open("sv_cl", O_RDONLY);
  if (sv_cl < 0) {
    perror("erro em open(sv_cl)");
    exit(-1);
  }

  if (argc > 4 && !strcmp("-a", argv[1])) {
    for (i = 0; argv[2][i] != '-' && !stop; i ++) {
      if (i > strlen(argv[2])) {
        stop = 1;
      }
    }
    if (!stop) strncpy (year, argv[2], i);
    year[i] = '\0';
    for (j = i + 1; argv[2][j] != '-' && !stop; j ++) {
      if (j > strlen(argv[2])) {
        stop = 1;
      }
    }
    if (!stop) {
      strncpy (month, &argv[2][i + 1], j - i - 1);
      month[j - i - 1] = '\0';
      strncpy (day, &argv[2][j + 1], strlen(argv[2]) - j - 1);
      day[strlen(argv[2]) - j - 1] = '\0';
    }
    for (i = 0; argv[3][i] != ':' && !stop; i ++) {
      if (i > strlen(argv[3])) {
        stop = 1;
      }
    }
    if (!stop) {
      strncpy (hour, argv[3], i);
      hour[i] = '\0';
    }
    for (j = i + 1; argv[3][j] != ':' && !stop; j ++) {
      if (j > strlen(argv[3])) {
        stop = 1;
      }
    }
    if (!stop) {
      strncpy (min, &argv[3][i + 1], j - i - 1);
      min[j - i - 1] = '\0';
      strncpy (sec, &argv[3][j + 1], strlen(argv[3]) - j - 1);
      sec[strlen(argv[3]) - j - 1] = '\0';
    }
    sched.tm_year = atoi(year) - 1900;
    sched.tm_mon = atoi(month) - 1;
    sched.tm_mday = atoi(day);
    sched.tm_hour = atoi(hour);
    sched.tm_min = atoi(min);
    sched.tm_sec = atoi(sec);
    sched.tm_isdst = -1;

    sched_t = mktime(&sched);
    sprintf(sched_s, "%ld", sched_t);
  }
  if (!fork()) {
    while ((size = read(sv_cl, buf, MAX)) > 0) {
      write(1, buf, size);
      write(1, "\n", 1);
    }
    exit(0);
  }
  else if (argc > 1) {
    strcpy(args, argv[1]);
    strcat(args, " ");
    if (argc > 4 && !strcmp("-a", argv[1]) && sched_t > time(NULL)) {
      strcat(args, sched_s);
      strcat(args, " ");
      for (int i = 4; i < argc; i ++) {
        strcat(args, argv[i]);
        if (i < argc - 1) {
          strcat(args, " ");
        }
      }
      write(cl_sv, args, strlen(args));
    }
    else if (argc == 3 && (!strcmp("-c", argv[1]) || !strcmp("-r", argv[1]) || !strcmp("-n", argv[1]) || !strcmp("-e", argv[1]))) {
      strcat(args, argv[2]);
      write(cl_sv, args, strlen(args));
    }
    else if (!strcmp("-l", argv[1])) {
      write(cl_sv, args, strlen(args));
    }
    else {
      write(1, "comando ou argumento invalido\n", 30);
    }
    exit(0);
  }
  else {
    write(1, "comando ou argumento invalido\n", 30);
    exit(0);
  }
  wait(NULL);
  close(cl_sv);
  close(sv_cl);

  return 0;
}
