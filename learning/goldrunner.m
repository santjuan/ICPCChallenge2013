close all;
clear all;

nhidden = 300; %number of neurons on the hidden layer
load('datos.mat');
[nt,m] = size(Xtrain);
nv = size(XCVal,1);

nn = createnn(m,nhidden,1);
compnn = @(nn1,X1)(regnn(nn1,X1));
errnn = @(nn1,X1,R1)(regerrnn(nn1,X1,R1));

wv=[nn.W(:);nn.V(:)];
nn = nntrain(nn,Xtrain,Ytrain,2000,compnn,errnn);
yp = regnn(nn,Xtrain);
cmp = [yp Ytrain];
cmp(1:20,:)

terr = sqrt(errnn(nn,Xtrain, Ytrain)/nt);
verr = sqrt(errnn(nn,XCVal, YCVal)/nv);
printf("Training Error: %f, Val. error: %f\n",terr,verr);
V = nn.V;
W = nn.W;
save("-binary","learned.mat","V","W");
